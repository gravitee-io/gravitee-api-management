import groovy.json.JsonSlurper


String header = '# Change Log\n\n'

def milestoneVersion = System.getProperties().getProperty('MILESTONE_VERSION')
def changelogFile = './release/changelog/CHANGELOG-v3.adoc'
def repo = 'issues'
println 'milestoneVersion' + milestoneVersion

def version = milestoneVersion.split(' - ')
def component = version[0]
def majorVersion = version[1].substring(0, 1) as Integer

def originChangelog
if (component == 'APIM') {
    header += 'For upgrade instructions, please refer to https://docs.gravitee.io/apim/' + majorVersion + '.x/apim_installguide_migration.html[APIM Migration Guide]\n\n'
    header += '*Important:* If you plan to skip versions when you upgrade, ensure that you read the version-specific upgrade notes for each intermediate version. You may be required to perform manual actions as part of the upgrade.\n\n'
}

originChangelog = readFile(changelogFile).replace(header, '')

String changelog = header

// get milestones from version
List milestones = new ArrayList()

for (int i = 1; i <= 150; i++) {
    def pageMilestones = new JsonSlurper().parseText(
            new URL('https://gh.gravitee.io/repos/gravitee-io/' + repo + '/milestones?state=closed&page=' + i).text)
    if (!pageMilestones) {
        break
    }
    milestones.addAll(pageMilestones)
}

def milestone = milestones.find { it.title == milestoneVersion }

if (milestone) {
    int milestoneNumber = milestone.number

    String milestoneDate = milestone.closed_at

    println 'Generating changelog for version ' + milestoneVersion + ' / milestone ' + milestoneNumber + '...'

    List issues = new ArrayList()

    for (int i = 1; i <= 100; i++) {
        def pageIssues = new JsonSlurper().parseText(
                new URL('https://gh.gravitee.io/repos/gravitee-io/' + repo + '/issues?state=closed&milestone=' + milestoneNumber + '&page=' + i).text)
        if (!pageIssues) {
            break
        }
        issues.addAll(pageIssues)
    }

    // exclusion of duplicates and technicals
    issues = issues.findAll {
        !it.labels.name.contains('type: duplicate') && !it.labels.name.contains('type: technical')
    }

    println issues.size + ' issues found'

    if (repo == 'issues') {
        changelog += '\n== https://github.com/gravitee-io/issues/milestone/' + milestoneNumber + '?closed=1[' + milestoneVersion + ' (' + milestoneDate.substring(0, 10) + ')]\n'
    } else {
        changelog += '\n== ' + milestoneVersion + ' (' + milestoneDate.substring(0, 10) + ')\n'
    }


    // Bug Fixes part
    changelog += generateChangelogPart(issues, 'Bug fixes', 'type: bug', repo)

    // Features part
    changelog += generateChangelogPart(issues, 'Features', 'type: feature', repo)

    // Improvements part
    changelog += generateChangelogPart(issues, 'Improvements', 'type: enhancement', repo)

    changelog += System.getProperty("line.separator") + ' '

    changelog += System.getProperty("line.separator") + ' '

    changelog += System.getProperty("line.separator")

    changelog += originChangelog

    writeFile file: changelogFile, text: changelog
} else {
    println 'Unknown version ' + milestoneVersion
}

private String generateChangelogPart(issues, String changelogPartTitle, String type, String repo) {
    String changelog = ''

    // filter type
    issues = issues.findAll { it.labels.name.contains(type) }

    println issues.size + ' issues found for the type ' + type

    if (issues) {
        changelog += '\n=== ' + changelogPartTitle + '\n'
        // group by domain (portal, gateway...)
        Map<String, List> domainIssues = new LinkedHashMap<String, List>()
        for (int i = 0; i < issues.size(); i++) {
            def matcher = issues[i].title =~ '^\\[((\\w|-|_)+)\\]'
            String domain = matcher.size() == 0 ? 'General' : matcher[0][1].capitalize()

            List listIssues = domainIssues.get(domain)

            if (listIssues == null) {
                listIssues = new ArrayList()
            }
            listIssues.add(issues[i])

            domainIssues.put(domain, listIssues)
        }

        domainIssues = domainIssues.sort()

        for (domainIssue in domainIssues.entrySet()) {
            changelog += '\n*_' + domainIssue.key + '_*\n\n'

            def iss = domainIssue.value
            List titles = new LinkedList()
            for (int j = 0; j < iss.size(); j++) {
                def title = iss[j].title
                if (iss[j].title.indexOf(']') > 0) {
                    title = title.substring(iss[j].title.indexOf(']') + 1)
                }

                if (repo == 'issues') {
                    titles.add('- ' + title.trim().replace(': ', '').capitalize() + ' ' + iss[j].html_url + '[#' + iss[j].number + ']\n')
                } else {
                    titles.add('- ' + title.trim().replace(': ', '').capitalize() + '\n')
                }
            }
            titles = titles.sort()

            for (int j = 0; j < titles.size(); j++) {
                changelog += titles[j]
            }
        }
    }
    return changelog
}

private static String readFile(String fileName) {
    return new File(fileName).text
}

private static void writeFile(object) {
    String file = object.file
    new File(file).text = object.text
}