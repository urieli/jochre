[#ftl]
<p><b>New correction on Jochre</b></p>
<ul>
<li><b>User:</b> ${correction.user}</li>
<li><b>IP:</b> ${correction.ip}</li>
<li><b>Date:</b> ${correction.createDate?datetime?string.full}</li>
<li><b>Original document:</b> ${correction.document.path}</li>
<li><b>Field:</b> ${correction.field}</li>
<li><b>Previous value:</b> ${correction.previousValue}</li>
<li><b>New value:</b> ${correction.value}</li>
[#if correction.applyEverywhere]
<li>Apply to all of the following documents:
<ul>
[#list correction.documents as doc]
  <li>${doc}</li>
[/#list]
</ul>
</li>
[/#if]
