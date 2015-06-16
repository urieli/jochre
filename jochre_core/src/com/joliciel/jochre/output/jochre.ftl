[#ftl]
<image pageIndex="${image.page.index?c}" imageIndex="${image.index?c}" width="${image.width?c}" height="${image.height?c}" lang="${image.page.document.locale.language}">
[#list image.paragraphs as paragraph]
[#if !paragraph.junk]
<paragraph l="${paragraph.left?c}" t="${paragraph.top?c}" r="${paragraph.right?c}" b="${paragraph.bottom?c}">
	[#list paragraph.rows as row]
		<row l="${row.left?c}" t="${row.top?c}" r="${row.right?c}" b="${row.bottom?c}">
		[#list row.groups as group]
			<word l="${group.left?c}" t="${group.top?c}" r="${group.right?c}" b="${group.bottom?c}" text="${group.word?replace("\"", "&quot;")}" known="[#if group.frequency > 0][#if group.split]split[#else]true[/#if][#else]false[/#if]">
			[#if group.wordFrequencies??]
			[#list group.wordFrequencies as wordFrequency]
				<freq text="${wordFrequency.outcome?replace("\"", "&quot;")}" weight="${wordFrequency.count?c}"/>
			[/#list]
			[/#if]
			[#list group.correctedShapes as shape]
				<char l="${shape.left?c}" t="${shape.top?c}" r="${shape.right?c}" b="${shape.bottom?c}" confidence="${(shape.confidence * 100.0)?round?c}" letter="${shape.letter?replace("\"", "&quot;")}" />
			[/#list]
			</word>
		[/#list]
		</row>
	[/#list]
</paragraph>
[/#if]
[/#list]
</image>

