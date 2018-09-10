[#ftl]
		<Page ID="PAGE${image.page.index?string["00000"]}_${image.index?string["0"]}" HEIGHT="${image.height?c}" WIDTH="${image.width?c}" PHYSICAL_IMG_NR="${image.page.index?c}">
			<PrintSpace HEIGHT="${image.printSpace.height?c}" WIDTH="${image.printSpace.width?c}" HPOS="${image.printSpace.left?c}" VPOS="${image.printSpace.top?c}" PC="${image.confidence?string["0.0000"]}">
				[#list image.paragraphs as paragraph]
				[#if !paragraph.junk]
					<TextBlock ID="PAR${image.page.index?string["00000"]}_${image.index?string["0"]}_${paragraph.index?string["000"]}" HEIGHT="${paragraph.height?c}" WIDTH="${paragraph.width?c}" HPOS="${paragraph.left?c}" VPOS="${paragraph.top?c}" ROTATION="${image.meanSlopeDegrees?string["0.00"]}" LANG="${image.page.document.locale.language}">
					[#list paragraph.rows as row]
						[#assign firstShape=row.shapes?first]
						[#if !image.leftToRight][#assign firstShape=row.shapes?last][/#if]
						<TextLine HEIGHT="${row.height?c}" WIDTH="${row.width?c}" HPOS="${row.left?c}" VPOS="${row.top?c}" BASELINE="${(firstShape.top+firstShape.baseLine)?c}">
						[#list row.groups as group]
							[#if group.index>0]<SP WIDTH="${group.precedingSpace.width?c}" HPOS="${group.precedingSpace.left?c}" VPOS="${group.precedingSpace.top?c}"/>[/#if]
							<String HEIGHT="${group.height?c}" WIDTH="${group.width?c}" HPOS="${group.left?c}" VPOS="${group.top?c}" CONTENT="${group.word?replace("\"", "&quot;")}">
							[#list group.correctedShapes as shape]
								<Glyph HEIGHT="${shape.height?c}" WIDTH="${shape.width?c}" HPOS="${shape.left?c}" VPOS="${shape.top?c}" CONTENT="${shape.letter?replace("\"", "&quot;")}" />
							[/#list]
							</String>
						[/#list]
						</TextLine>
					[/#list]
					</TextBlock>
				[/#if]
				[/#list]
			</PrintSpace>
		</Page>
		