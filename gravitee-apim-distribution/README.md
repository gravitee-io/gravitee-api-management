# Analyze APIM distribution

## Introduction
After each release, the size of the APIM distribution can increase.
Most of the time, it's normal.

But because of some mistakes in pom.xml files, some ZIP plugins can be bigger than necessary.
Some libraries can also be imported more than once.

The purpose of the analyze script is to list all libraries:
 - in `lib/ext` folder
 - in each `lib` folder of each ZIP plugins

As a result, the script count how many times a library is present in the distribution, where it is, and its size.
The goal is to use this data to identify what we can remove from ZIP to reduce their size.

## Usage
From the project base dir, build an APIM distribution:
`mvn clean package`

Then, execute the analyze script:
`sh analyze-distrib.sh`

It will generate 2 csv files:
 - gateway.logs
 - rest-apis.logs

These two files have to be imported in this Google Sheet: http://docs.google.com/spreadsheets/d/1TyC2dsog-rAD3ACmTgXMy_vZ9hlDTXSA3f1zuTQg8Kk/edit?usp=sharing

## Next steps

Based on the GoogleSheet, we can easily identified that some libraries are embedded in many plugins whereas they are already present in the `lib/ext` folder of the mAPI & GW distribution.

A first step will be to make new versions of these plugins, removing unnecessary dependencies.

But some plugins are not present by default in the bundle, so for them we will have to keep "duplicated" libraries. It's indeed useless to add external libraries if the plugins that depend on are not loaded.
