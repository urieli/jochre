[#ftl]
		<Page ID="PAGE${image.page.index?string["00000"]}_${image.index?string["0"]}" HEIGHT="${image.height?c}" WIDTH="${image.width?c}" PHYSICAL_IMG_NR="${image.page.index?c}">
			<PrintSpace HEIGHT="${image.printSpace.height?c}" WIDTH="${image.printSpace.width?c}" HPOS="${image.printSpace.left?c}" VPOS="${image.printSpace.top?c}" PC="${image.confidence?string["0.0000"]}">
				[#list image.paragraphs as paragraph]
				[#if !paragraph.junk]
					[#assign hyphenated=false]
					<TextBlock xmlns:ns1="http://www.w3.org/1999/xlink" ID="PAR${image.page.index?string["00000"]}_${image.index?string["0"]}_${paragraph.index?string["000"]}" HEIGHT="${paragraph.height?c}" WIDTH="${paragraph.width?c}" HPOS="${paragraph.left?c}" VPOS="${paragraph.top?c}" ns1:type="simple" language="${image.page.document.locale.language}">
					[#list paragraph.rows as row]
						<TextLine HEIGHT="${row.height?c}" WIDTH="${row.width?c}" HPOS="${row.left?c}" VPOS="${row.top?c}">
						[#list row.groups as group]
							[#if group.index>0]<SP WIDTH="${group.precedingSpace.width?c}" HPOS="${group.precedingSpace.left?c}" VPOS="${group.precedingSpace.top?c}"/>[/#if]
							[#list group.subsequences as subsequence][#assign rect=subsequence.getRectangleInGroup(group)]
							[#if rect??]
							<String HEIGHT="${rect.height?c}" WIDTH="${rect.width?c}" HPOS="${rect.left?c}" VPOS="${rect.top?c}" CONTENT="${subsequence.guessedWord?replace("\"", "&quot;")}" [#if subsequence.softHyphenSubsequence??]SUBS_TYPE="HypPart1" SUBS_CONTENT="${subsequence.wordFrequencies[0].outcome?replace("\"", "&quot;")}"[#elseif hyphenated][#assign hyphenated=false]SUBS_TYPE="HypPart2" SUBS_CONTENT="${hyphenatedContent?replace("\"", "&quot;")}"[/#if]  WC="${group.confidence?string["0.0000"]}">
							[#if subsequence.wordFrequencies[0]??][#assign otherWord=subsequence.wordFrequencies[0].outcome][#if otherWord!=subsequence.guessedWord]
								<ALTERNATIVE>${otherWord?replace("\"", "&quot;")}</ALTERNATIVE>
							[/#if][/#if]
							</String>
							[#if subsequence.softHyphenSubsequence??][#assign hyphenated=true][#assign hyphenatedContent=subsequence.wordFrequencies[0].outcome][#assign hypRect=subsequence.softHyphenSubsequence.getRectangleInGroup(group)][#if hypRect??]<HYP HEIGHT="${hypRect.height?c}" WIDTH="${hypRect.width?c}" HPOS="${hypRect.left?c}" VPOS="${hypRect.top?c}" CONTENT="-"/>[/#if][/#if]
							[/#if]
							[/#list]
						[/#list]
						</TextLine>
					[/#list]
					</TextBlock>
				[/#if]
				[/#list]
			</PrintSpace>
		</Page>
		