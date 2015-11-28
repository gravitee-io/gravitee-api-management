@errors = require './errors'
@loader = require './loader'
util    = require './util'

class @FileError extends @errors.MarkedYAMLError

class @FileReader
  constructor: (readFileAsyncOverride) ->
    @q    = require 'q'
    @url  = require 'url'

    if readFileAsyncOverride
      @readFileAsyncOverride = readFileAsyncOverride

  ###
  Read file either locally or from the network.
  ###
  readFileAsync: (file) ->
    if @readFileAsyncOverride
      return @readFileAsyncOverride(file)

    if (/^https?/i).test(file) or window?
      return @fetchFileAsync file

    return @fetchLocalFileAsync file

  ###
  Read file from the disk.
  ###
  fetchLocalFileAsync: (file) ->
    deferred = @q.defer()
    require('fs').readFile file, (err, data) =>
      if (err)
        deferred.reject(new exports.FileError "while reading #{file}", null, "cannot read #{file} (#{err})", @start_mark)
      else
        deferred.resolve(data.toString())
    return deferred.promise

  ###
  Read file from the network.
  ###
  fetchFileAsync: (file) ->
    if window?
      return @fetchFileAsyncBrowser file

    @fetchFileAsyncNode file

  fetchFileAsyncNode: (file) ->
    deferred = @q.defer()

    require('got').get(file, {
      headers: {
        'Accept': 'application/raml+yaml, */*'
      }
    }, (err, data, res) ->
      if err
        return deferred.reject(new exports.FileError "while fetching #{file}", null, "cannot fetch #{file} (#{err.message})", @start_mark)

      if res.statusCode == 200 or res.statusCode == 304
        return deferred.resolve(data)

      return deferred.reject(new exports.FileError "while fetching #{file}", null, "cannot fetch #{file} (#{res.statusCode})", @start_mark)
    )

    deferred.promise

  fetchFileAsyncBrowser: (file) ->
    deferred = @q.defer()
    xhr = new XMLHttpRequest()

    try
      xhr.open 'GET', file
    catch error
      throw new exports.FileError "while fetching #{file}", null, "cannot fetch #{file} (#{error}), check that the server is up and that CORS is enabled", @start_mark

    xhr.setRequestHeader 'Accept', 'application/raml+yaml, */*'
    xhr.onreadystatechange = () =>
      if (xhr.readyState == 4)
        if (xhr.status == 200 or xhr.status == 304)
          deferred.resolve(xhr.responseText)
        else
          deferred.reject(new exports.FileError "while fetching #{file}", null, "cannot fetch #{file} (#{xhr.status})", @start_mark)
    xhr.send()

    deferred.promise

###
OO version of the parser, static functions will be removed after consumers move on to use the OO version
OO will offer caching
###
class @RamlParser
  constructor: (@settings = defaultSettings) ->
    @q    = require 'q'
    @url  = require 'url'
    @nodes= require './nodes'
    @loadDefaultSettings(@settings)

  loadDefaultSettings: (settings) ->
    Object.keys(defaultSettings).forEach (settingName) =>
      unless settingName of settings
        settings[settingName] = defaultSettings[settingName]

  loadFile: (file, settings = @settings) ->
    try
      return settings.reader.readFileAsync(file)
      .then (stream) =>
        @load stream, file, settings
    catch error
      # return a promise that throws the error
      return @q.fcall =>
        throw new exports.FileError "while fetching #{file}", null, "cannot fetch #{file} (#{error})", null

  composeFile: (file, settings = @settings, parent) ->
    try
      return settings.reader.readFileAsync(file)
      .then (stream) =>
        @compose stream, file, settings, parent
    catch error
      # return a promise that throws the error
      return @q.fcall =>
        throw new exports.FileError "while fetching #{file}", null, "cannot fetch #{file} (#{error})", null

  compose: (stream, location, settings = @settings, parent = { src: location}) ->
    settings.compose = false
    @parseStream(stream, location, settings, parent)

  load: (stream, location, settings = @settings) ->
    settings.compose = true
    @parseStream(stream, location, settings,  { src: location})

  parseStream: (stream, location, settings = @settings, parent) ->
    loader = new exports.loader.Loader stream, location, settings, parent

    return @q.fcall =>
      return loader.getYamlRoot()

    .then (partialTree) =>
      files = loader.getPendingFilesList()
      @getPendingFiles(loader, partialTree, files)

    .then (fullyAssembledTree) =>
      loader.composeRamlTree(fullyAssembledTree, settings)

      if settings.compose
        if fullyAssembledTree?
          return loader.construct_document(fullyAssembledTree)
        else
          return null
      else
        return fullyAssembledTree

  getPendingFiles: (loader, node, files) ->
    return node if !files.length

    loadFiles = files.map((fileInfo) =>
      @getPendingFile(loader, fileInfo)
    )

    return @q.all(loadFiles)
      .then((results) =>
        lastVisitedNode = undefined

        results.forEach((result, index) =>
          overwritingnode = @mergePendingFile(files[index], result)

          # we should be able to handle parentless children, but only the last one
          if overwritingnode and !lastVisitedNode
            lastVisitedNode = overwritingnode
        )

        return if lastVisitedNode then lastVisitedNode else node
      )

  getPendingFile: (loader, fileInfo) ->
    event   = fileInfo.event
    fileUri = fileInfo.targetFileUri

    if loader.parent and @isInIncludeTagsStack(fileUri, loader)
      return @q.reject(new exports.FileError 'while composing scalar out of !include', null, "detected circular !include of #{event.value}", event.start_mark)

    return @settings.reader.readFileAsync(fileUri)
      .then((fileData) =>
        if fileInfo.type is 'fragment'
          @compose(fileData, fileInfo.targetFileUri, { validate: false, transform: false, compose: true }, loader)
        else
          new @nodes.ScalarNode('tag:yaml.org,2002:str', fileData, event.start_mark, event.end_mark, event.style)
      )
      .catch((error) =>
        @addContextToError(error, event)
      )

  mergePendingFile: (fileInfo, value) ->
    node = fileInfo.parentNode
    key  = fileInfo.parentKey

    return @appendNewNodeToParent(node, key, value)

  addContextToError: (error, event) ->
    if error.constructor.name == "FileError"
      unless error.problem_mark
        error.problem_mark = event.start_mark
      throw error
    else
      throw new exports.FileError 'while reading file', null, error, event.start_mark

  # detect
  isInIncludeTagsStack:  (include, parent) ->
    while parent = parent.parent
      if parent.src is include
        return true
    return false

  appendNewNodeToParent: (node, key, value) ->
    if node
      if util.isSequence(node)
        node.value[key] = value
      else
        node.value.push [key, value]
      return null
    else
      return value

###
  validate controls whether the stream must be processed as a
###
defaultSettings = { validate: true, transform: true, compose: true, reader: new exports.FileReader(null) }

###
Parse the first RAML document in a stream and produce the corresponding
Javascript object.
###
@loadFile = (file, settings = defaultSettings) ->
  parser = new exports.RamlParser(settings)
  parser.loadFile(file, settings)

###
Parse the first RAML document in a file and produce the corresponding
representation tree.
###
@composeFile = (file, settings = defaultSettings, parent = file) ->
  parser = new exports.RamlParser(settings)
  parser.composeFile(file, settings, parent)

###
Parse the first RAML document in a stream and produce the corresponding
representation tree.
###
@compose = (stream, location, settings = defaultSettings, parent = location) ->
  parser = new exports.RamlParser(settings)
  parser.compose(stream, location, settings, parent)

###
Parse the first RAML document in a stream and produce the corresponding
Javascript object.
###
@load = (stream, location, settings = defaultSettings) ->
  parser = new exports.RamlParser(settings)
  parser.load(stream, location, settings, null)
