var keyboardMappings = {};
var keyboardMappingEnabled = false;
var authorCounter = 0;

$(function() {
	applyKeyboardMappings();

	$('#updateKeyboardMappings').click(function() {
		loadKeyboardMappings();
		return false;
	});

	$('#txtAuthor').typeahead({
		hint: false,
		highlight: true,
		minLength: 1
	},
	{
		name: 'authors',
		limit: 100,
		source: function (q, sync, async) {
			console.log(q);
			$.getJSON( `${JOCHRE_SEARCH_EXT_URL}?command=prefixSearch&prefix=${q}&field=author&maxResults=8`, function( matches ) {
				async(matches)
			});
		}
	});

	$('#addAuthor').click(function() {
		var author = $('#txtAuthor').val();
		if (author.length > 0) {
			console.log(`adding ${author}`);
			authorCounter += 1;
			$('#authorList').append(`
				<div id="newAuthor${authorCounter}" class="alert alert-success alert-dismissible p-0 alert-auto">
					&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${author}
					<button type="button" class="close p-0" data-dismiss="alert" aria-label="Close">&times;</button>
					<script>
						$('#newAuthor${authorCounter}').on('closed.bs.alert', function () {
							var currentAuthors = $('#hdnAuthors').val().replace('|${author}','');
							console.log(currentAuthors);
							$('#hdnAuthors').val(currentAuthors);
						})
					<\/script>
				</div>&nbsp;
				`);
			$('#txtAuthor').typeahead('val','');
			var currentAuthors = $('#hdnAuthors').val();
			$('#hdnAuthors').val(`${currentAuthors}|${author}`);
		}
	});

	$("#toggleAdvancedSearch").click(function(){
		$("#advancedSearch").toggle();
	});

	$("#txtQuery").keypress(function(evt){
		return transformKeyPress(this, evt);
	});
	$("#txtTitle").keypress(function(evt){
		return transformKeyPress(this, evt);
	});
	$("#txtAuthor").keypress(function(evt){
		if(evt.which == 13) {
			$('#addAuthor').click();
			return false;
		}
		return transformKeyPress(this, evt);
	});

	$("#txtSuggestion").keypress(function(evt){
		return transformKeyPress(this, evt);
	});

	$("#txtSuggestion2").keypress(function(evt){
		return transformKeyPress(this, evt);
	});

	$('#imgFixWord').on('load', function() {
		$("#imgWordLoading").hide();
	});

	$('#selFont').on('change', function() {
		$("#imgFont").attr("src", `${STATIC_LOCATION}images/${this.value}.png`);
	});

	$('#btnSaveFixWord').click( function (e) {
		applyFix();
	});

	$('#btnKeyboardDefault').click( function() {
		$('#frmKeysAction').val("default");
		$.ajax({
			type: 'POST',
			url: '/updateKeyboard',
			data: $('#frmKeys').serialize(),
			success: function() {
				$('#alertKeyboardError').hide();
				$('#alertKeyboardSuccess').show();
				applyKeyboardMappings();
			},
			error: function(XMLHttpRequest, textStatus, errorThrown) {
				$('#alertKeyboardError').show();
				$('#alertKeyboardSuccess').hide();
			},
		});
	});

	$('#btnKeyboardSave').click( function() {
		$('#frmKeysAction').val("save");
		$.ajax({
			type: 'POST',
			url: '/updateKeyboard',
			data: $('#frmKeys').serialize(),
			success: function() {
				$('#alertKeyboardError').hide();
				$('#alertKeyboardSuccess').show();
				applyKeyboardMappings();
			},
			error: function(XMLHttpRequest, textStatus, errorThrown) {
				$('#alertKeyboardError').show();
				$('#alertKeyboardSuccess').hide();
			},
		});
	});

	$('.alert .close').click(function(){
		$(this).parent().hide();
		var modal = $(this).closest('.modal');
		if (modal)
			modal.modal('hide');
	});
});

function applyKeyboardMappings() {
	keyboardMappings = {};
	keyboardMappingEnabled = false;
	$.getJSON( `/keyboard`, function( data ) {
		$.each( data, function( key, val ) {
			if (key=="mapping") {
				$.each( val, function( from, to ) {
					keyboardMappings[from] = to;
				});
			} else if (key=="enabled") {
				keyboardMappingEnabled = val;
			}
		});
	});
}

function transformChar(charStr) {
	if (charStr in keyboardMappings)
		return keyboardMappings[charStr];
	else
		return "";
}

function transformKeyPress(textfield, evt) {
	if (!keyboardMappingEnabled)
		return true;

	var val = textfield.value;
	evt = evt || window.event;

	if (evt.ctrlKey)
		return true;

	// Ensure we only handle prindiv class="container" keys, excluding enter and space
	var charCode = typeof evt.which == "number" ? evt.which : evt.keyCode;
	if (charCode && charCode > 32) {
		var keyChar = String.fromCharCode(charCode);

		// Transform typed character
		var mappedChar = transformChar(keyChar);
		if (mappedChar=="")
			return true;

		var start = textfield.selectionStart;
		var end = textfield.selectionEnd;
		var newValue = val.slice(0, start) + mappedChar + val.slice(end);
		if (textfield.id=='txtAuthor')
			$('#txtAuthor').typeahead('val',newValue);
		else
			textfield.value = newValue;

		// Move the caret
		textfield.selectionStart = textfield.selectionEnd = start + mappedChar.length;

		return false;
	}
};

function fixWord(evt, docId) {
	var sel = window.getSelection();
	
	var range = sel.getRangeAt(0);
	console.log(`range startOffset: ${range.startOffset}, endOffset ${range.endOffset}, startContainer ${range.startContainer.nodeName}, endContainer ${range.endContainer.nodeName}, commonAncestorContainer ${range.commonAncestorContainer.nodeName}`);
	console.log(`sel.anchorNode: ${sel.anchorNode.nodeName}, sel.anchorOffset: ${sel.anchorOffset}`);

	var wordOffset;
	// was a text node selected?
	if (sel.anchorNode.nodeType == 3) {
		var localOffset = sel.anchorOffset;
		var globalOffset = parseInt($(sel.anchorNode).closest("span[offset]").attr("offset"));
		wordOffset = globalOffset + localOffset;
		console.log(`localOffset: ${localOffset}, globalOffset: ${globalOffset}, wordOffset: ${wordOffset}`);
	} else {
		var childNumber = sel.anchorOffset;
		var span = sel.anchorNode.children[childNumber];
		wordOffset = parseInt($(span).closest("span[offset]").attr("offset"));
		console.log(`wordOffset: ${wordOffset}`);
	}
	$('#alertFixWordSuccess').hide();
	$('#alertFixWordError').hide();

	$('#fixWordModal').data('docId', docId);
	$('#fixWordModal').data('wordOffset', wordOffset);
	
	$("#imgFixWord").attr("src","");
	$("#imgWordLoading").show();
	$("#txtSuggestion").val("");
	$("#txtSuggestion2").val("");
	$("#txtSuggestion2").hide();
	$("#imgFixWord").attr("src",`${JOCHRE_SEARCH_EXT_URL}?command=wordImage&docId=${docId}&startOffset=${wordOffset}`);
	
	$("#selFont").val($("#selFont option:first").val());
	$("#selLang").val($("#selLang option:first").val());
	
	$("#imgFont").attr("src", `${STATIC_LOCATION}images/${$('#selFont').val()}.png`);
	
	$.getJSON( `${JOCHRE_SEARCH_EXT_URL}?command=word&docId=${docId}&startOffset=${wordOffset}`, function( data ) {
			$.each( data, function( key, val ) {
				if (key=="word") {
					console.log(`found word : ${val}`);
					$("#txtSuggestion").val(val);
				} else if (key=="word2") {
					console.log(`found word2 : ${val}`);
					$("#txtSuggestion2").val(val);
					$("#txtSuggestion2").show();
				}
			});
		});

	$("#fixWordModal").modal();
}

function applyFix() {
	var docId = $('#fixWordModal').data('docId');
	var wordOffset = $('#fixWordModal').data('wordOffset');

	$("#imgWordLoading").show();

	var suggestion = $("#txtSuggestion").val();
	var suggestion2 = $("#txtSuggestion2").val();
	
	var selFont = document.getElementById("selFont");
	var fontCode = selFont.options[selFont.selectedIndex].value;

	var selLang = document.getElementById("selLang");
	var languageCode = selLang.options[selLang.selectedIndex].value;
	
	console.log(`Apply fix ${suggestion} for ${docId} at ${wordOffset}`);
	
	$.ajax({
		url: JOCHRE_SEARCH_EXT_URL + "?command=suggest"
			+ "&docId=" + docId
			+ "&startOffset=" + wordOffset
			+ "&user=" + USERNAME
			+ "&ip=" + IP
			+ "&suggestion=" + encodeURIComponent(suggestion)
			+ "&suggestion2=" + encodeURIComponent(suggestion2)
			+ "&fontCode=" + encodeURIComponent(fontCode)
			+ "&languageCode=" + encodeURIComponent(languageCode),
		dataType: 'json',
		success: function( data ) {
			$("#imgWordLoading").hide();
			$('#alertFixWordError').hide();
			$('#alertFixWordSuccess').show();
		},
		error: function(XMLHttpRequest, textStatus, errorThrown) {
			$("#imgWordLoading").hide();
			$('#alertFixWordError').show();
			$('#alertFixWordSuccess').hide();
			},
		});
}

function loadKeyboardMappings() {
	$('#keyboardEntries').empty();
	$('#alertKeyboardError').hide();
	$('#alertKeyboardSuccess').hide();
	$.getJSON( `/keyboard`, function( data ) {
		var mappings = [];
		var enabled;
		$.each( data, function( key, val ) {
			if (key=="mapping") {
				$.each( val, function( from, to ) {
					mappings.push([from, to]);
				});
			} else if (key=="enabled") {
				enabled = val;
			}
		});
		$('#frmKeysEnabled').prop('checked', enabled);
		for (var i=0; i<mappings.length; i++) {
			var key = mappings[i][0];
			var val = mappings[i][1];
			var htmlVal = val.replace(/&/g, '&amp;')
				.replace(/"/g, '&quot;')
				.replace(/'/g, '&#39;')
				.replace(/</g, '&lt;')
				.replace(/>/g, '&gt;');

			var button = "";
			if (i==mappings.length-1) {
				button = `<button class="btn btn-success btn-add" type="button">+</button>`;
			} else {
				button = `<button class="btn btn-danger btn-remove" type="button">-</button>`;
			}
			$('#keyboardEntries').append(`
				<div class="entry input-group col-3">
					<input class="form-control" name="from" type="text" placeholder="From" value="${key}" />
					<input class="form-control" name="to" type="text" placeholder="To" value="${htmlVal}" />
					<span class="input-group-btn">
						${button}
					</span>
				</div>
				`);
		}
		$("#keyboardModal").modal()
	});
}