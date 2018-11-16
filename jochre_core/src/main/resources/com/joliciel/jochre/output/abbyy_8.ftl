[#ftl]
<page width="${image.width?c}" height="${image.height?c}" resolution="600" originalCoords="true">
[#list image.paragraphs as paragraph]
[#if !paragraph.junk]
<block blockType="Text" l="${paragraph.left?c}" t="${paragraph.top?c}" r="${paragraph.right?c}" b="${paragraph.bottom?c}">
  <text>
  <par lineSpacing="-1">
  [#list paragraph.rows as row]
    <line baseline="${row.bottom?c}" l="${row.left?c}" t="${row.top?c}" r="${row.right?c}" b="${row.bottom?c}">
    <formatting lang="Yiddish" ff="Times New Roman" fs="12." bold="false" spacing="-1">
      [#list row.groups as group]
        [#assign firstShape=true]
        [#list group.correctedShapes as shape]
          <charParams l="${shape.left?c}" t="${shape.top?c}" r="${shape.right?c}" b="${shape.bottom?c}" wordStart="${firstShape?string}" wordFromDictionary="[#if group.frequency > 0]true[#else]false[/#if]" wordNormal="true" wordNumeric="false" wordIdentifier="false" charConfidence="${(shape.confidence * 100.0)?round?c}" serifProbability="100" wordPenalty="0" meanStrokeWidth="${shape.width?c}">${shape.letter}</charParams>
          [#assign firstShape=false]
        [/#list]
        [#if (lastGroup??)]
          [#assign spaceLeft = lastGroup.right]
          [#assign spaceTop = group.top]
          [#assign spaceRight = group.left]
          [#assign spaceBottom = group.bottom]
          [#if lastGroup.left > group.left]
            [#assign spaceLeft=group.right]
            [#assign spaceRight=lastGroup.left]
          [/#if]
          <charParams l="${spaceLeft?c}" t="${spaceTop?c}" r="${spaceRight?c}" b="${spaceBottom?c}" wordStart="false" wordFromDictionary="false" wordNormal="false" wordNumeric="false" wordIdentifier="false" charConfidence="255" serifProbability="255" wordPenalty="0" meanStrokeWidth="0"> </charParams>
        [/#if]
        [#assign lastGroup=group]
      [/#list]
    </formatting>
    </line>
  [/#list]
  </par>
  </text>
</block>
[/#if]
[/#list]
</page>

