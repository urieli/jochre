select doc_name, page_index, shape_letter from ocr_shape inner join ocr_group on shape_group_id = group_id
inner join ocr_row on group_row_id = row_id
inner join ocr_paragraph on row_paragraph_id = paragraph_id
inner join ocr_image on paragraph_image_id = image_id
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where shape_letter='o'
order by doc_name, page_index, shape_letter;
