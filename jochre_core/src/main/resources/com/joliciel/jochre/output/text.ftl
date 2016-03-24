[#ftl]
[#list image.paragraphs as paragraph]
[#list paragraph.rows as row]
[#list row.groups as group][#if group.bestLetterSequence??]${group.bestLetterSequence.guessedWord} [/#if][/#list]
[/#list]

[/#list]
