var keyboardMappings = {};
var keyboardMappingEnabled = false;
var authorCounter = 0;
var useKeyboardMappings = USE_KEYBOARD_MAPPINGS;

$(function() {
  if (useKeyboardMappings) {
    applyKeyboardMappings();

    $('#updateKeyboardMappings').click(function() {
      loadKeyboardMappings();
      return false;
    });
  }

  $('#updatePreferences').click(function() {
    loadPreferences();
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
        <div id="newAuthor${authorCounter}" class="alert alert-info alert-dismissible alert-auto col-auto py-2 mx-2">
          ${author}
          <button type="button" class="close py-2" data-dismiss="alert" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
          <script>
            $('#newAuthor${authorCounter}').on('closed.bs.alert', function () {
              var currentAuthors = $('#hdnAuthors').val().replace('|${author}','');
              console.log(currentAuthors);
              $('#hdnAuthors').val(currentAuthors);
            })
          <\/script>
        </div>
        `);
      $('#txtAuthor').typeahead('val','');
      var currentAuthors = $('#hdnAuthors').val();
      $('#hdnAuthors').val(`${currentAuthors}|${author}`);
    }
  });

  $("#toggleAdvancedSearch").click(function(e){
    e.preventDefault();
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
    hideDiv($("#imgWordLoading"));
  });

  $('#selFont').on('change', function() {
    $("#imgFont").attr("src", `${STATIC_LOCATION}images/${$('#selLang').val()}_${this.value}.png`);
  });

  $('#btnSaveFixWord').click( function (e) {
    applyFix();
  });

  $('#btnCorrectMetaSave').click( function (e) {
    applyCorrection();
  });

  $('#btnKeyboardDefault').click( function() {
    $('#frmKeysAction').val("default");
    $.ajax({
      type: 'POST',
      url: '/updateKeyboard',
      data: $('#frmKeys').serialize(),
      success: function() {
        hideDiv($('#alertKeyboardError'));
        showDiv($('#alertKeyboardSuccess'));
        applyKeyboardMappings();
      },
      error: function(XMLHttpRequest, textStatus, errorThrown) {
        showDiv($('#alertKeyboardError'));
        hideDiv($('#alertKeyboardSuccess'));
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
        hideDiv($('#alertKeyboardError'));
        showDiv($('#alertKeyboardSuccess'));
        applyKeyboardMappings();
      },
      error: function(XMLHttpRequest, textStatus, errorThrown) {
        showDiv($('#alertKeyboardError'));
        hideDiv($('#alertKeyboardSuccess'));
      },
    });
  });

  $('#btnDefaultPrefs').click( function() {
    $('#frmPrefsAction').val("default");
    $.ajax({
      type: 'POST',
      url: '/updatePreferences',
      data: $('#frmPrefs').serialize(),
      success: function() {
        hideDiv($('#alertPrefsError'));
        showDiv($('#alertPrefsSuccess'));
      },
      error: function(XMLHttpRequest, textStatus, errorThrown) {
        showDiv($('#alertPrefsError'));
        hideDiv($('#alertPrefsSuccess'));
      },
    });
  });

  $('#btnSavePrefs').click( function() {
    $('#frmPrefsAction').val("save");
    $.ajax({
      type: 'POST',
      url: '/updatePreferences',
      data: $('#frmPrefs').serialize(),
      success: function() {
        hideDiv($('#alertPrefsError'));
        showDiv($('#alertPrefsSuccess'));
        location.reload();
      },
      error: function(XMLHttpRequest, textStatus, errorThrown) {
        showDiv($('#alertPrefsError'));
        hideDiv($('#alertPrefsSuccess'));
      },
    });
  });

  $('.alert .close').click(function(){
    hideDiv($(this).parent());
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
  if (!useKeyboardMappings)
    return true;

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
    else if (textfield.id=='correctMetaAuthorMergeWith')
      $('#correctMetaAuthorMergeWith').typeahead('val',newValue);
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

  hideDiv($('#alertFixWordSuccess'));
  hideDiv($('#alertFixWordError'));

  $('#fixWordModal').data('docId', docId);
  $('#fixWordModal').data('wordOffset', wordOffset);

  $("#imgFixWord").attr("src","");
  showDiv($("#imgWordLoading"));
  $("#txtSuggestion").val("");
  $("#txtSuggestion2").val("");
  hideDiv($("#txtSuggestion2").parents('.row').first());
  $("#imgFixWord").attr("src",`${JOCHRE_SEARCH_EXT_URL}?command=wordImage&docId=${docId}&startOffset=${wordOffset}`);

  $("#selFont").val($("#selFont option:first").val());
  $("#selLang").val($("#selLang option:first").val());

  $("#imgFont").attr("src", `${STATIC_LOCATION}images/${$('#selLang').val()}_${$('#selFont').val()}.png`);

  $.getJSON( `${JOCHRE_SEARCH_EXT_URL}?command=word&docId=${docId}&startOffset=${wordOffset}`, function( data ) {
      $.each( data, function( key, val ) {
        if (key=="word") {
          console.log(`found word : ${val}`);
          $("#txtSuggestion").val(val);
        } else if (key=="word2") {
          console.log(`found word2 : ${val}`);
          $("#txtSuggestion2").val(val);
          showDiv($("#txtSuggestion2").parents('.row').first());
        }
      });
    });

  $("#fixWordModal").modal();
}

function applyFix() {
  var docId = $('#fixWordModal').data('docId');
  var wordOffset = $('#fixWordModal').data('wordOffset');

  showDiv($("#imgWordLoading"));

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
      hideDiv($("#imgWordLoading"));
      showDiv($('#alertFixWordSuccess'));
      hideDiv($('#alertFixWordError'));
    },
    error: function(XMLHttpRequest, textStatus, errorThrown) {
      hideDiv($("#imgWordLoading"));
      hideDiv($('#alertFixWordSuccess'));
      showDiv($('#alertFixWordError'));
      },
    });
}

function correctMeta(docId, field, fieldForDisplay, rtl, value) {
  $('#correctMetaModal').data('docId', docId);
  $('#correctMetaModal').data('field', field);
  $('#correctMetaFieldName').text(fieldForDisplay);
  $('#correctMetaCurrentValue').text(value);
  $('#correctMetaNewValue').val(value);
  $('#correctMetaMergeWith').val("");
  $('#replaceOrMerge1').prop("checked", true);
  $('#correctMetaNewValue').removeAttr("disabled");
  $('#correctMetaAuthorMergeWith').attr("disabled", "true");
  $('#correctMetaAuthorEnglishMergeWith').attr("disabled", "true");

  $('input[type=radio][name=replaceOrMerge]').on('change', function() {
     switch($(this).val()) {
       case 'replace':
         $('#correctMetaNewValue').removeAttr("disabled");
         $('#correctMetaAuthorMergeWith').attr("disabled", "true");
         $('#correctMetaAuthorEnglishMergeWith').attr("disabled", "true");
         break;
       case 'merge':
         $('#correctMetaNewValue').attr("disabled", "true");
         $('#correctMetaAuthorMergeWith').removeAttr("disabled");
         $('#correctMetaAuthorEnglishMergeWith').removeAttr("disabled");
         break;
     }
  });

  if (rtl) {
    $('#correctMetaFieldName').addClass("rtl");
    $('#correctMetaCurrentValue').addClass("rtl");
    $('#correctMetaNewValue').addClass("rtl");
    $("#correctMetaNewValue").keypress(function(evt){
      return transformKeyPress(this, evt);
    });
    $('#correctMetaAuthorMergeWith').addClass("rtl");
    $("#correctMetaAuthorMergeWith").keypress(function(evt){
      return transformKeyPress(this, evt);
    });
  } else {
    $('#correctMetaFieldName').removeClass("rtl");
    $('#correctMetaCurrentValue').removeClass("rtl");
    $('#correctMetaNewValue').removeClass("rtl");
    $("#correctMetaNewValue").off('keypress');
    $('#correctMetaAuthorMergeWith').removeClass("rtl");
    $("#correctMetaAuthorMergeWith").off('keypress');
  }

  if (field==="author") {
    $('#correctMetaAuthorMergeDiv').removeClass('d-none');
    $('#correctMetaAuthorMergeDiv').addClass('d-flex');
    $('#correctMetaAuthorMergeWith').typeahead({
      hint: false,
      highlight: true,
      minLength: 1
    },
    {
      name: `correctMeta-${field}`,
      limit: 100,
      source: function (q, sync, async) {
        console.log(q);
        $.getJSON( `${JOCHRE_SEARCH_EXT_URL}?command=prefixSearch&prefix=${q}&field=${field}&maxResults=8`, function( matches ) {
          async(matches)
        });
      }
    });
    $('.replaceOrMerge').addClass('d-flex');
    $('.replaceOrMerge').removeClass('d-none');
    $('#correctMetaAuthorEnglishMergeDiv').addClass('d-none');
    $('#correctMetaAuthorEnglishMergeDiv').removeClass('d-flex');
  } else if (field==="authorEnglish") {
    $('#correctMetaAuthorEnglishMergeDiv').removeClass('d-none');
    $('#correctMetaAuthorEnglishMergeDiv').addClass('d-flex');
    $('#correctMetaAuthorEnglishMergeWith').typeahead({
      hint: false,
      highlight: true,
      minLength: 1
    },
    {
      name: `correctMeta-${field}`,
      limit: 100,
      source: function (q, sync, async) {
        console.log(q);
        $.getJSON( `${JOCHRE_SEARCH_EXT_URL}?command=prefixSearch&prefix=${q}&field=${field}&maxResults=8`, function( matches ) {
          async(matches)
        });
      }
    });
    $('.replaceOrMerge').addClass('d-flex');
    $('.replaceOrMerge').removeClass('d-none');
    $('#correctMetaAuthorMergeDiv').addClass('d-none');
    $('#correctMetaAuthorMergeDiv').removeClass('d-flex');
  } else {
    $('#correctMetaAuthorMergeDiv').addClass('d-none');
    $('#correctMetaAuthorMergeDiv').removeClass('d-flex');
    $('#correctMetaAuthorEnglishMergeDiv').addClass('d-none');
    $('#correctMetaAuthorEnglishMergeDiv').removeClass('d-flex');
    $('.replaceOrMerge').addClass('d-none');
    $('.replaceOrMerge').removeClass('d-flex');
  }

  $("#correctMetaModal").modal();
}

function applyCorrection() {
  let docId = $('#correctMetaModal').data('docId');
  let field = $('#correctMetaModal').data('field');
  let replaceOrMerge = $('input[name=replaceOrMerge]:checked').val();

  let value = $('#correctMetaNewValue').val();
  if (replaceOrMerge==="merge") {
    value = $('#correctMetaAuthorMergeWith').val();
    if (field==="authorEnglish")
      value = $('#correctMetaAuthorEnglishMergeWith').val();
  }

  let applyEverywhere = (replaceOrMerge==="merge");

  console.log(`Apply correction for ${docId}, ${field}: ${value}`);

  $.ajax({
    url: JOCHRE_SEARCH_EXT_URL + "?command=correct"
      + "&docId=" + docId
      + "&user=" + USERNAME
      + "&ip=" + IP
      + "&field=" + encodeURIComponent(field)
      + "&suggestion=" + encodeURIComponent(value)
      + "&applyEverywhere=" + applyEverywhere,
    dataType: 'json',
    success: function( data ) {
      hideDiv($('#alertCorrectMetaError'));
      showDiv($('#alertCorrectMetaSuccess'));
    },
    error: function(XMLHttpRequest, textStatus, errorThrown) {
      showDiv($('#alertCorrectMetaError'));
      hideDiv($('#alertCorrectMetaSuccess'));
      },
    });
}

function loadKeyboardMappings() {
  $('#keyboardEntries').empty();
  hideDiv($('#alertKeyboardError'));
  hideDiv($('#alertKeyboardSuccess'));
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

function loadPreferences() {
  hideDiv($('#alertPrefsError'));
  hideDiv($('#alertPrefsSuccess'));
  $.getJSON( `/preferences`, function( data ) {
    $.each( data, function( key, val ) {
      if (key=="docsPerPage") {
        $("#docsPerPage").val(val);
      } else if (key=="snippetsPerDoc") {
        $("#snippetsPerDoc").val(val);
      }
    });
    $("#preferencesModal").modal()
  });
}

function showDiv(div) {
  div.removeClass('d-none');
  div.addClass('d-flex');
}

function hideDiv(div) {
  div.addClass('d-none');
  div.removeClass('d-flex');
}