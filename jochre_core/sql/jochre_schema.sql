--
-- PostgreSQL database dump
--

-- Dumped from database version 8.4.4
-- Dumped by pg_dump version 9.2.3
-- Started on 2015-02-13 17:25:31

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- TOC entry 533 (class 2612 OID 16386)
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: postgres
--

CREATE OR REPLACE PROCEDURAL LANGUAGE plpgsql;


ALTER PROCEDURAL LANGUAGE plpgsql OWNER TO postgres;

SET search_path = public, pg_catalog;

--
-- TOC entry 179 (class 1255 OID 5000136)
-- Name: copy_document(integer, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION copy_document(p_source_doc_id integer, p_target_doc_id integer) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
	l_shape RECORD;
	l_source_group_id INTEGER;
	l_target_group_id INTEGER;
	l_source_row_id INTEGER;
	l_target_row_id INTEGER;
	l_current_row_id INTEGER;
	l_source_paragraph_id INTEGER;
	l_target_paragraph_id INTEGER;
	l_current_paragraph_id INTEGER;
	l_source_image_id INTEGER;
	l_target_image_id INTEGER;
	l_current_image_id INTEGER;
	l_source_page_id INTEGER;
	l_target_page_id INTEGER;
	l_current_page_id INTEGER;
	l_index INTEGER;
BEGIN
RAISE NOTICE 'Starting';

l_source_row_id := 0;
l_target_row_id := 0;
l_current_row_id := 0;
l_source_paragraph_id := 0;
l_target_paragraph_id := 0;
l_current_paragraph_id := 0;
l_source_image_id := 0;
l_target_image_id := 0;
l_current_image_id := 0;
l_source_page_id := 0;
l_target_page_id := 0;
l_current_page_id := 0;
l_source_group_id := 0;
l_target_group_id := 0;
FOR l_shape IN 
	(SELECT page_id, image_id, paragraph_id, row_id, group_id, shape_letter, shape_original_guess, shape_index FROM ocr_shape
	INNER JOIN ocr_group ON shape_group_id = group_id
	INNER JOIN ocr_row ON group_row_id = row_id
	INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id
	INNER JOIN ocr_image ON paragraph_image_id = image_id
	INNER JOIN ocr_page ON image_page_id = page_id
	INNER JOIN ocr_document ON page_doc_id = doc_id
	WHERE doc_id = p_source_doc_id
	ORDER BY doc_id, page_index, image_index, paragraph_index, row_index, group_index, shape_index)
LOOP	
	IF (l_shape.group_id != l_source_group_id) THEN
		IF (l_shape.row_id != l_source_row_id) THEN
			l_target_row_id = l_current_row_id;
		END IF;
		IF (l_shape.paragraph_id != l_source_paragraph_id) THEN
			l_target_paragraph_id = l_current_paragraph_id;
		END IF;
		IF (l_shape.image_id != l_source_image_id) THEN
			l_target_image_id = l_current_image_id;
		END IF;
		IF (l_shape.page_id != l_source_page_id) THEN
			l_target_page_id = l_current_page_id;
		END IF;
		
		SELECT INTO l_target_group_id MIN(group_id) FROM ocr_group
		INNER JOIN ocr_row ON group_row_id = row_id
		INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id
		INNER JOIN ocr_image ON paragraph_image_id = image_id
		INNER JOIN ocr_page ON image_page_id = page_id
		INNER JOIN ocr_document ON page_doc_id = doc_id
		WHERE doc_id = p_target_doc_id
		AND group_id > l_target_group_id
		AND row_id > l_target_row_id
		AND paragraph_id > l_target_paragraph_id
		AND image_id > l_target_image_id
		AND page_id > l_target_page_id;

		SELECT INTO l_current_row_id, l_current_paragraph_id, l_current_image_id, l_current_page_id row_id, paragraph_id, image_id, page_id
		FROM ocr_group
		INNER JOIN ocr_row ON group_row_id = row_id
		INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id
		INNER JOIN ocr_image ON paragraph_image_id = image_id
		INNER JOIN ocr_page ON image_page_id = page_id
		INNER JOIN ocr_document ON page_doc_id = doc_id
		WHERE group_id = l_target_group_id;

		l_source_group_id = l_shape.group_id;
		l_source_row_id = l_shape.row_id;
		l_source_paragraph_id = l_shape.paragraph_id;
		l_source_image_id = l_shape.image_id;
		l_source_page_id = l_shape.page_id;

		RAISE NOTICE 'source group % , target group %', l_source_group_id , l_target_group_id;
	END IF;
	UPDATE ocr_shape SET shape_letter = l_shape.shape_letter, shape_original_guess = l_shape.shape_original_guess
	WHERE shape_group_id = l_target_group_id
	AND shape_index = l_shape.shape_index;
END LOOP;

RETURN;
END;
$$;


ALTER FUNCTION public.copy_document(p_source_doc_id integer, p_target_doc_id integer) OWNER TO postgres;

--
-- TOC entry 180 (class 1255 OID 5000137)
-- Name: copy_document_letters(integer, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION copy_document_letters(p_source_doc_id integer, p_target_doc_id integer) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
BEGIN
RAISE NOTICE 'Starting';

-- update document authors
insert into ocr_doc_author_map (docauthor_doc_id, docauthor_author_id)
select p_target_doc_id, docauthor_author_id from ocr_doc_author_map where docauthor_doc_id=p_source_doc_id;

-- update document information
update ocr_document as d1 set doc_filename=d2.doc_filename,
  doc_locale=d2.doc_locale,
  doc_owner_id=d2.doc_owner_id,
  doc_name_local=d2.doc_name_local,
  doc_publisher=d2.doc_publisher,
  doc_city=d2.doc_city,
  doc_year=d2.doc_year,
  doc_reference=d2.doc_reference
  from ocr_document d2
  where d1.doc_id=p_target_doc_id and d2.doc_id=p_source_doc_id;

-- blank out all the letters
update ocr_shape as s1 set shape_letter='■'
from ocr_group g1
inner join ocr_row r1 on g1.group_row_id = r1.row_id
inner join ocr_paragraph pr1 on r1.row_paragraph_id = pr1.paragraph_id
inner join ocr_image i1 on pr1.paragraph_image_id = i1.image_id
inner join ocr_page p1 on i1.image_page_id = p1.page_id and p1.page_doc_id=p_target_doc_id
where s1.shape_group_id = g1.group_id;

-- copy exact matches from one document to another
update ocr_shape as s1 set shape_original_guess = s2.shape_original_guess, shape_letter = s2.shape_letter
from ocr_group g1
inner join ocr_row r1 on g1.group_row_id = r1.row_id
inner join ocr_paragraph pr1 on r1.row_paragraph_id = pr1.paragraph_id
inner join ocr_image i1 on pr1.paragraph_image_id = i1.image_id
inner join ocr_page p1 on i1.image_page_id = p1.page_id and p1.page_doc_id=p_target_doc_id
inner join ocr_page p2 on p1.page_index = p2.page_index and p2.page_doc_id=p_source_doc_id
inner join ocr_image i2 on i2.image_page_id = p2.page_id 
inner join ocr_paragraph pr2 on pr2.paragraph_image_id = i2.image_id
inner join ocr_row r2 on r2.row_paragraph_id = pr2.paragraph_id
inner join ocr_group g2 on g2.group_row_id = r2.row_id
inner join ocr_shape s2 on s2.shape_group_id = g2.group_id
where s1.shape_group_id = g1.group_id
--and length(s1.shape_letter) = 0
and s2.shape_top = s1.shape_top and s2.shape_bottom = s1.shape_bottom and s2.shape_left = s1.shape_left and s2.shape_right = s1.shape_right;

insert into ocr_split (split_id, split_shape_id, split_position)
select nextval('ocr_split_id_seq'), s1.shape_id, sp2.split_position
from ocr_shape as s1
inner join ocr_group g1 on s1.shape_group_id = g1.group_id
inner join ocr_row r1 on g1.group_row_id = r1.row_id
inner join ocr_paragraph pr1 on r1.row_paragraph_id = pr1.paragraph_id
inner join ocr_image i1 on pr1.paragraph_image_id = i1.image_id
inner join ocr_page p1 on i1.image_page_id = p1.page_id and p1.page_doc_id=p_target_doc_id
inner join ocr_page p2 on p1.page_index = p2.page_index and p2.page_doc_id=p_source_doc_id
inner join ocr_image i2 on i2.image_page_id = p2.page_id 
inner join ocr_paragraph pr2 on pr2.paragraph_image_id = i2.image_id
inner join ocr_row r2 on r2.row_paragraph_id = pr2.paragraph_id
inner join ocr_group g2 on g2.group_row_id = r2.row_id
inner join ocr_shape s2 on s2.shape_group_id = g2.group_id
inner join ocr_split sp2 on s2.shape_id = sp2.split_shape_id
where s1.shape_group_id = g1.group_id
--and length(s1.shape_letter) = 0
and s2.shape_top = s1.shape_top and s2.shape_bottom = s1.shape_bottom and s2.shape_left = s1.shape_left and s2.shape_right = s1.shape_right;


RETURN;
END;
$$;


ALTER FUNCTION public.copy_document_letters(p_source_doc_id integer, p_target_doc_id integer) OWNER TO postgres;

--
-- TOC entry 181 (class 1255 OID 5000138)
-- Name: copy_image_letters(integer, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION copy_image_letters(p_source_image_id integer, p_target_image_id integer) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
	l_shape RECORD;
	l_source_group_id INTEGER;
	l_target_group_id INTEGER;
	l_source_row_id INTEGER;
	l_target_row_id INTEGER;
	l_current_row_id INTEGER;
	l_source_paragraph_id INTEGER;
	l_target_paragraph_id INTEGER;
	l_current_paragraph_id INTEGER;
	l_index INTEGER;
	l_text character varying (256);
	l_row_changed INTEGER;
	l_paragraph_changed INTEGER;
BEGIN
RAISE NOTICE 'Starting';

l_source_row_id := 0;
l_target_row_id := 0;
l_current_row_id := 0;
l_source_paragraph_id := 0;
l_target_paragraph_id := 0;
l_current_paragraph_id := 0;
l_source_group_id := 0;
l_target_group_id := 0;
FOR l_shape IN 
	(SELECT paragraph_id, row_id, group_id, shape_letter, shape_original_guess, shape_index FROM ocr_shape
	INNER JOIN ocr_group ON shape_group_id = group_id
	INNER JOIN ocr_row ON group_row_id = row_id
	INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id
	INNER JOIN ocr_image ON paragraph_image_id = image_id
	WHERE image_id = p_source_image_id
	ORDER BY image_index, paragraph_index, row_index, group_index, shape_index)
LOOP	
	IF (l_shape.group_id != l_source_group_id) THEN
		RAISE NOTICE 'text: %', l_text;
		l_row_changed := 0;
		l_paragraph_changed := 0;
		
		l_text := '';
		RAISE NOTICE 'shape group %, shape row % , shape paragraph %', l_shape.group_id, l_shape.row_id , l_shape.paragraph_id;
		RAISE NOTICE 'current row %, current paragraph %', l_current_row_id , l_current_paragraph_id;
		IF (l_shape.row_id != l_source_row_id) THEN
			l_row_changed := 1;
			l_target_row_id := l_current_row_id;
		END IF;
		IF (l_shape.paragraph_id != l_source_paragraph_id) THEN
			l_paragraph_changed := 1;
			l_target_paragraph_id := l_current_paragraph_id;
		END IF;
		RAISE NOTICE 'target group %, target row %, target paragraph %', l_target_group_id , l_target_row_id, l_target_paragraph_id;

		if (l_row_changed = 1 and l_paragraph_changed = 1) then
			SELECT INTO l_target_group_id MIN(group_id) FROM ocr_group
			INNER JOIN ocr_row ON group_row_id = row_id
			INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id
			INNER JOIN ocr_image ON paragraph_image_id = image_id
			WHERE image_id = p_target_image_id
			AND paragraph_id > l_current_paragraph_id;
		elsif (l_row_changed = 1) then
			SELECT INTO l_target_group_id MIN(group_id) FROM ocr_group
			INNER JOIN ocr_row ON group_row_id = row_id
			INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id
			INNER JOIN ocr_image ON paragraph_image_id = image_id
			WHERE image_id = p_target_image_id
			AND row_id > l_current_row_id
			AND paragraph_id = l_current_paragraph_id;
		else
			SELECT INTO l_target_group_id MIN(group_id) FROM ocr_group
			INNER JOIN ocr_row ON group_row_id = row_id
			INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id
			INNER JOIN ocr_image ON paragraph_image_id = image_id
			WHERE image_id = p_target_image_id
			AND group_id > l_target_group_id
			AND row_id = l_current_row_id
			AND paragraph_id = l_current_paragraph_id;
		end if;

		RAISE NOTICE 'new target group %', l_target_group_id;

		if (l_target_group_id is not null) then
			SELECT INTO l_current_row_id, l_current_paragraph_id row_id, paragraph_id
			FROM ocr_group
			INNER JOIN ocr_row ON group_row_id = row_id
			INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id
			INNER JOIN ocr_image ON paragraph_image_id = image_id
			WHERE group_id = l_target_group_id;
		end if;

		l_source_group_id := l_shape.group_id;
		l_source_row_id := l_shape.row_id;
		l_source_paragraph_id := l_shape.paragraph_id;
		RAISE NOTICE 'source group % , source row %, source paragraph %', l_source_group_id , l_source_row_id, l_source_paragraph_id;

	END IF;
	l_text := l_text || l_shape.shape_letter;
	
	UPDATE ocr_shape SET shape_letter = l_shape.shape_letter, shape_original_guess = l_shape.shape_original_guess
	WHERE shape_group_id = l_target_group_id
	AND shape_index = l_shape.shape_index;
END LOOP;

RETURN;
END;
$$;


ALTER FUNCTION public.copy_image_letters(p_source_image_id integer, p_target_image_id integer) OWNER TO postgres;

--
-- TOC entry 182 (class 1255 OID 5000139)
-- Name: delete_document(integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION delete_document(p_doc_id integer) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
delete from ocr_split where split_shape_id in (
select shape_id from ocr_shape
inner join ocr_group on shape_group_id = group_id
inner join ocr_row on group_row_id = row_id
inner join ocr_paragraph on row_paragraph_id = paragraph_id
inner join ocr_image on paragraph_image_id = image_id
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id);

delete from ocr_shape where shape_group_id in (
select group_id from ocr_group
inner join ocr_row on group_row_id = row_id
inner join ocr_paragraph on row_paragraph_id = paragraph_id
inner join ocr_image on paragraph_image_id = image_id
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id);

delete from ocr_group where group_row_id in (
select row_id from ocr_row
inner join ocr_paragraph on row_paragraph_id = paragraph_id
inner join ocr_image on paragraph_image_id = image_id
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id);

delete from ocr_row where row_paragraph_id in (
select paragraph_id from ocr_paragraph
inner join ocr_image on paragraph_image_id = image_id
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id);

delete from ocr_paragraph where paragraph_image_id in (
select image_id from ocr_image
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id);

delete from ocr_image where image_page_id in (
select page_id from ocr_page
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id);

delete from ocr_page where page_doc_id=p_doc_id;

delete from ocr_doc_author_map where docauthor_doc_id=p_doc_id;

delete from ocr_document where doc_id=p_doc_id;
RETURN;
END;
$$;


ALTER FUNCTION public.delete_document(p_doc_id integer) OWNER TO postgres;

--
-- TOC entry 183 (class 1255 OID 5000140)
-- Name: delete_page(integer, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION delete_page(p_doc_id integer, p_page_index integer) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
delete from ocr_split where split_shape_id in (
select shape_id from ocr_shape
inner join ocr_group on shape_group_id = group_id
inner join ocr_row on group_row_id = row_id
inner join ocr_paragraph on row_paragraph_id = paragraph_id
inner join ocr_image on paragraph_image_id = image_id
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id
and page_index=p_page_index);

delete from ocr_shape where shape_group_id in (
select group_id from ocr_group
inner join ocr_row on group_row_id = row_id
inner join ocr_paragraph on row_paragraph_id = paragraph_id
inner join ocr_image on paragraph_image_id = image_id
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id
and page_index=p_page_index);

delete from ocr_group where group_row_id in (
select row_id from ocr_row
inner join ocr_paragraph on row_paragraph_id = paragraph_id
inner join ocr_image on paragraph_image_id = image_id
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id
and page_index=p_page_index);

delete from ocr_row where row_paragraph_id in (
select paragraph_id from ocr_paragraph
inner join ocr_image on paragraph_image_id = image_id
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id
and page_index=p_page_index);

delete from ocr_paragraph where paragraph_image_id in (
select image_id from ocr_image
inner join ocr_page on image_page_id = page_id
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id
and page_index=p_page_index);

delete from ocr_image where image_page_id in (
select page_id from ocr_page
inner join ocr_document on page_doc_id = doc_id
where doc_id=p_doc_id
and page_index=p_page_index);

delete from ocr_page where page_doc_id=p_doc_id
and page_index=p_page_index;

RETURN;
END;
$$;


ALTER FUNCTION public.delete_page(p_doc_id integer, p_page_index integer) OWNER TO postgres;

--
-- TOC entry 184 (class 1255 OID 5000141)
-- Name: populate_words(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION populate_words() RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
	l_shape RECORD;
	l_current_group_id INTEGER;
	l_count INTEGER;
	l_text CHARACTER VARYING(256);
	l_index INTEGER;
BEGIN
DELETE FROM ocr_word;
ALTER SEQUENCE ocr_word_id_seq RESTART WITH 1;

l_current_group_id := 0;
l_index := 0;
FOR l_shape IN 
	(SELECT shape_group_id, shape_letter FROM ocr_shape
	INNER JOIN ocr_group ON shape_group_id = group_id
	INNER JOIN ocr_row ON group_row_id = row_id
	INNER JOIN ocr_paragraph ON row_paragraph_id = paragraph_id
	INNER JOIN ocr_image ON paragraph_image_id = image_id
	INNER JOIN ocr_page ON image_page_id = page_id
	INNER JOIN ocr_document ON page_doc_id = doc_id
	WHERE image_imgstatus_id = 2
	ORDER BY doc_id, page_index, image_index, paragraph_index, row_index, group_index, shape_index)
LOOP	
	EXIT WHEN l_index >= 10000;
	IF (l_shape.shape_group_id != l_current_group_id) THEN
		IF (l_current_group_id!=0) THEN
			SELECT INTO l_count count(*) FROM ocr_word
			WHERE word_text = l_text;

			IF (l_count>0) THEN
				UPDATE ocr_word SET word_frequency = word_frequency+1
				WHERE WORD_TEXT = l_text;
			ELSE
				INSERT INTO ocr_word (word_id, word_text, word_frequency) VALUES (nextval('ocr_word_id_seq'), l_text, 1);
			END IF;

			
		END IF;
		l_text := '';
		l_current_group_id = l_shape.shape_group_id;
	END IF;
	IF (l_shape.shape_letter IS NOT NULL) THEN
		l_text := l_text || l_shape.shape_letter;
	END IF;
	l_index := l_index + 1;
END LOOP;

RETURN;
END;
$$;


ALTER FUNCTION public.populate_words() OWNER TO postgres;

--
-- TOC entry 185 (class 1255 OID 5000142)
-- Name: trig_login(); Type: FUNCTION; Schema: public; Owner: nlp
--

CREATE FUNCTION trig_login() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        -- make use of the special variable TG_OP to work out the operation.
        IF (TG_OP = 'UPDATE') THEN
            IF (NEW.user_logins!=OLD.user_logins) THEN
		INSERT INTO ocr_login (login_id, login_time, login_user_id, login_success) VALUES (nextval('ocr_login_id_seq'), now(), OLD.user_id, true);
            END IF;
            IF (NEW.user_failed_logins!=OLD.user_failed_logins) THEN
		INSERT INTO ocr_login (login_id, login_time, login_user_id, login_success) VALUES (nextval('ocr_login_id_seq'), now(), OLD.user_id, false);
            END IF;
        END IF;
        RETURN NULL; -- result is ignored since this is an AFTER trigger
    END
$$;


ALTER FUNCTION public.trig_login() OWNER TO nlp;

--
-- TOC entry 186 (class 1255 OID 5000143)
-- Name: wipedb(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION wipedb() RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
delete from ocr_shape_feature;
delete from ocr_shape;
delete from ocr_feature;
delete from ocr_group;
delete from ocr_row;
delete from ocr_paragraph;
delete from ocr_image;
delete from ocr_page;
delete from ocr_document;

ALTER SEQUENCE ocr_doc_id_seq RESTART WITH 1;
ALTER SEQUENCE ocr_page_id_seq RESTART WITH 1;
ALTER SEQUENCE ocr_image_id_seq RESTART WITH 1;
ALTER SEQUENCE ocr_paragraph_id_seq RESTART WITH 1;
ALTER SEQUENCE ocr_row_id_seq RESTART WITH 1;
ALTER SEQUENCE ocr_group_id_seq RESTART WITH 1;
ALTER SEQUENCE ocr_shape_id_seq RESTART WITH 1;
ALTER SEQUENCE ocr_feature_id_seq RESTART WITH 1;
ALTER SEQUENCE ocr_shpftr_id_seq RESTART WITH 1;

insert into ocr_document (doc_id, doc_name, doc_filename, doc_locale)
values (nextval('ocr_doc_id_seq'), 'doc', 'docfilename', 'yi');

insert into ocr_page (page_id, page_doc_id, page_index)
values (nextval('ocr_page_id_seq'), 1, 0);

insert into ocr_image (image_id, image_name, image_width, image_height, image_black_threshold, image_page_id, image_index)
values (nextval('ocr_image_id_seq'), 'image', 100, 100, 100, 1, 0);

insert into ocr_paragraph (paragraph_id, paragraph_image_id, paragraph_index)
values (nextval('ocr_paragraph_id_seq'), 1, 0);

insert into ocr_row (row_id, row_paragraph_id, row_index) values (nextval('ocr_row_id_seq'), 1, 0);

insert into ocr_group (group_id, group_row_id, group_index) values (nextval('ocr_group_id_seq'), 1, 0);

RETURN;
END;
$$;


ALTER FUNCTION public.wipedb() OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 140 (class 1259 OID 5000144)
-- Name: ocr_author; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_author (
    author_id integer NOT NULL,
    author_last_name character varying(256),
    author_first_name character varying(256),
    author_last_name_local character varying(256),
    author_first_name_local character varying(256)
);


ALTER TABLE public.ocr_author OWNER TO nlp;

--
-- TOC entry 141 (class 1259 OID 5000150)
-- Name: ocr_author_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_author_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_author_id_seq OWNER TO nlp;

--
-- TOC entry 142 (class 1259 OID 5000152)
-- Name: ocr_doc_author_map; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_doc_author_map (
    docauthor_doc_id integer NOT NULL,
    docauthor_author_id integer NOT NULL
);


ALTER TABLE public.ocr_doc_author_map OWNER TO nlp;

--
-- TOC entry 143 (class 1259 OID 5000155)
-- Name: ocr_doc_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_doc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_doc_id_seq OWNER TO nlp;

--
-- TOC entry 144 (class 1259 OID 5000157)
-- Name: ocr_document; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_document (
    doc_id integer NOT NULL,
    doc_filename character varying(512) NOT NULL,
    doc_name character varying(1024) NOT NULL,
    doc_locale character varying(10) NOT NULL,
    doc_owner_id integer NOT NULL,
    doc_name_local character varying(1024),
    doc_publisher character varying(1024),
    doc_city character varying(256),
    doc_year smallint,
    doc_reference character varying(256)
);


ALTER TABLE public.ocr_document OWNER TO nlp;

--
-- TOC entry 145 (class 1259 OID 5000163)
-- Name: ocr_group; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_group (
    group_id integer NOT NULL,
    group_row_id integer NOT NULL,
    group_index smallint NOT NULL,
    group_hard_hyphen boolean DEFAULT false NOT NULL,
    group_broken_word boolean DEFAULT false NOT NULL,
    group_segment_problem boolean DEFAULT false NOT NULL,
    group_skip boolean DEFAULT false NOT NULL
);


ALTER TABLE public.ocr_group OWNER TO nlp;

--
-- TOC entry 146 (class 1259 OID 5000169)
-- Name: ocr_group_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_group_id_seq OWNER TO nlp;

--
-- TOC entry 147 (class 1259 OID 5000171)
-- Name: ocr_image; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_image (
    image_id integer NOT NULL,
    image_name character varying(500) NOT NULL,
    image_width smallint NOT NULL,
    image_height smallint NOT NULL,
    image_black_threshold smallint NOT NULL,
    image_page_id integer NOT NULL,
    image_index smallint DEFAULT 0 NOT NULL,
    image_sep_threshold smallint NOT NULL,
    image_black_limit smallint NOT NULL,
    image_white_limit smallint NOT NULL,
    image_white_gap_fill_factor smallint DEFAULT 0 NOT NULL,
    image_imgstatus_id smallint NOT NULL,
    image_image bytea,
    image_owner_id integer NOT NULL
);


ALTER TABLE public.ocr_image OWNER TO nlp;

--
-- TOC entry 148 (class 1259 OID 5000179)
-- Name: ocr_image_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_image_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_image_id_seq OWNER TO nlp;

--
-- TOC entry 149 (class 1259 OID 5000181)
-- Name: ocr_image_status; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_image_status (
    imgstatus_id smallint NOT NULL,
    imgstatus_name character varying(50) NOT NULL
);


ALTER TABLE public.ocr_image_status OWNER TO nlp;

--
-- TOC entry 150 (class 1259 OID 5000184)
-- Name: ocr_login; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_login (
    login_id integer NOT NULL,
    login_time timestamp without time zone NOT NULL,
    login_user_id integer NOT NULL,
    login_success boolean NOT NULL
);


ALTER TABLE public.ocr_login OWNER TO nlp;

--
-- TOC entry 151 (class 1259 OID 5000187)
-- Name: ocr_login_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_login_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_login_id_seq OWNER TO nlp;

--
-- TOC entry 152 (class 1259 OID 5000189)
-- Name: ocr_page; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_page (
    page_id integer NOT NULL,
    page_doc_id integer NOT NULL,
    page_index smallint NOT NULL
);


ALTER TABLE public.ocr_page OWNER TO nlp;

--
-- TOC entry 153 (class 1259 OID 5000192)
-- Name: ocr_page_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_page_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_page_id_seq OWNER TO nlp;

--
-- TOC entry 154 (class 1259 OID 5000194)
-- Name: ocr_paragraph; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_paragraph (
    paragraph_id integer NOT NULL,
    paragraph_image_id integer NOT NULL,
    paragraph_index smallint DEFAULT 0 NOT NULL
);


ALTER TABLE public.ocr_paragraph OWNER TO nlp;

--
-- TOC entry 155 (class 1259 OID 5000198)
-- Name: ocr_paragraph_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_paragraph_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_paragraph_id_seq OWNER TO nlp;

--
-- TOC entry 156 (class 1259 OID 5000200)
-- Name: ocr_param; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_param (
    param_id integer DEFAULT 1 NOT NULL,
    param_last_failed_login timestamp with time zone NOT NULL,
    param_captcha_interval integer NOT NULL
);


ALTER TABLE public.ocr_param OWNER TO nlp;

--
-- TOC entry 157 (class 1259 OID 5000204)
-- Name: ocr_row; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_row (
    row_id integer NOT NULL,
    row_paragraph_id integer NOT NULL,
    row_index smallint NOT NULL,
    row_image bytea,
    row_height smallint
);


ALTER TABLE public.ocr_row OWNER TO nlp;

--
-- TOC entry 158 (class 1259 OID 5000210)
-- Name: ocr_row_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_row_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_row_id_seq OWNER TO nlp;

--
-- TOC entry 159 (class 1259 OID 5000212)
-- Name: ocr_shape; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_shape (
    shape_id integer NOT NULL,
    shape_top smallint NOT NULL,
    shape_left smallint NOT NULL,
    shape_bottom smallint NOT NULL,
    shape_right smallint NOT NULL,
    shape_cap_line smallint,
    shape_mean_line smallint,
    shape_base_line smallint,
    shape_letter character varying(20),
    shape_index smallint NOT NULL,
    shape_group_id integer NOT NULL,
    shape_pixels bytea,
    shape_original_guess character varying(20)
);


ALTER TABLE public.ocr_shape OWNER TO nlp;

--
-- TOC entry 1946 (class 0 OID 0)
-- Dependencies: 159
-- Name: TABLE ocr_shape; Type: COMMENT; Schema: public; Owner: nlp
--

COMMENT ON TABLE ocr_shape IS 'a single shape representing a single letter';


--
-- TOC entry 1947 (class 0 OID 0)
-- Dependencies: 159
-- Name: COLUMN ocr_shape.shape_letter; Type: COMMENT; Schema: public; Owner: nlp
--

COMMENT ON COLUMN ocr_shape.shape_letter IS 'the letter represented by this shape';


--
-- TOC entry 160 (class 1259 OID 5000218)
-- Name: ocr_shape_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_shape_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_shape_id_seq OWNER TO nlp;

--
-- TOC entry 161 (class 1259 OID 5000220)
-- Name: ocr_split; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_split (
    split_id integer NOT NULL,
    split_shape_id integer NOT NULL,
    split_position smallint NOT NULL
);


ALTER TABLE public.ocr_split OWNER TO nlp;

--
-- TOC entry 162 (class 1259 OID 5000223)
-- Name: ocr_split_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_split_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_split_id_seq OWNER TO nlp;

--
-- TOC entry 163 (class 1259 OID 5000225)
-- Name: ocr_user; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_user (
    user_id integer NOT NULL,
    user_username character varying(256) NOT NULL,
    user_password character varying(256) NOT NULL,
    user_first_name character varying(256) NOT NULL,
    user_last_name character varying(256) NOT NULL,
    user_role smallint NOT NULL,
    user_failed_logins integer DEFAULT 0 NOT NULL,
    user_logins integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.ocr_user OWNER TO nlp;

--
-- TOC entry 164 (class 1259 OID 5000233)
-- Name: ocr_user_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_user_id_seq
    START WITH 2
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_user_id_seq OWNER TO nlp;

--
-- TOC entry 165 (class 1259 OID 5000235)
-- Name: ocr_word; Type: TABLE; Schema: public; Owner: nlp; Tablespace: 
--

CREATE TABLE ocr_word (
    word_id integer NOT NULL,
    word_text character varying(256) NOT NULL,
    word_frequency integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.ocr_word OWNER TO nlp;

--
-- TOC entry 166 (class 1259 OID 5000239)
-- Name: ocr_word_id_seq; Type: SEQUENCE; Schema: public; Owner: nlp
--

CREATE SEQUENCE ocr_word_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ocr_word_id_seq OWNER TO nlp;

--
-- TOC entry 1876 (class 2606 OID 5000350)
-- Name: pk_author; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_author
    ADD CONSTRAINT pk_author PRIMARY KEY (author_id);


--
-- TOC entry 1880 (class 2606 OID 5000352)
-- Name: pk_doc; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_document
    ADD CONSTRAINT pk_doc PRIMARY KEY (doc_id);


--
-- TOC entry 1878 (class 2606 OID 5000354)
-- Name: pk_doc_author; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_doc_author_map
    ADD CONSTRAINT pk_doc_author PRIMARY KEY (docauthor_doc_id, docauthor_author_id);


--
-- TOC entry 1882 (class 2606 OID 5000356)
-- Name: pk_group; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_group
    ADD CONSTRAINT pk_group PRIMARY KEY (group_id);


--
-- TOC entry 1890 (class 2606 OID 5000358)
-- Name: pk_imgstatus; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_image_status
    ADD CONSTRAINT pk_imgstatus PRIMARY KEY (imgstatus_id);


--
-- TOC entry 1892 (class 2606 OID 5000360)
-- Name: pk_login; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_login
    ADD CONSTRAINT pk_login PRIMARY KEY (login_id);


--
-- TOC entry 1886 (class 2606 OID 5000362)
-- Name: pk_ocr_image; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_image
    ADD CONSTRAINT pk_ocr_image PRIMARY KEY (image_id);


--
-- TOC entry 1904 (class 2606 OID 5000364)
-- Name: pk_ocr_row; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_row
    ADD CONSTRAINT pk_ocr_row PRIMARY KEY (row_id);


--
-- TOC entry 1908 (class 2606 OID 5000366)
-- Name: pk_ocr_shape; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_shape
    ADD CONSTRAINT pk_ocr_shape PRIMARY KEY (shape_id);


--
-- TOC entry 1894 (class 2606 OID 5000368)
-- Name: pk_page; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_page
    ADD CONSTRAINT pk_page PRIMARY KEY (page_id);


--
-- TOC entry 1898 (class 2606 OID 5000370)
-- Name: pk_paragraph; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_paragraph
    ADD CONSTRAINT pk_paragraph PRIMARY KEY (paragraph_id);


--
-- TOC entry 1902 (class 2606 OID 5000372)
-- Name: pk_param; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_param
    ADD CONSTRAINT pk_param PRIMARY KEY (param_id);


--
-- TOC entry 1914 (class 2606 OID 5000374)
-- Name: pk_split; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_split
    ADD CONSTRAINT pk_split PRIMARY KEY (split_id);


--
-- TOC entry 1918 (class 2606 OID 5000376)
-- Name: pk_user; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_user
    ADD CONSTRAINT pk_user PRIMARY KEY (user_id);


--
-- TOC entry 1922 (class 2606 OID 5000378)
-- Name: pk_word; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_word
    ADD CONSTRAINT pk_word PRIMARY KEY (word_id);


--
-- TOC entry 1884 (class 2606 OID 5000380)
-- Name: uk_group_on_row; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_group
    ADD CONSTRAINT uk_group_on_row UNIQUE (group_row_id, group_index);


--
-- TOC entry 1888 (class 2606 OID 5000382)
-- Name: uk_image_on_page; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_image
    ADD CONSTRAINT uk_image_on_page UNIQUE (image_page_id, image_index);


--
-- TOC entry 1896 (class 2606 OID 5000384)
-- Name: uk_page; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_page
    ADD CONSTRAINT uk_page UNIQUE (page_doc_id, page_index);


--
-- TOC entry 1900 (class 2606 OID 5000386)
-- Name: uk_paragraph; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_paragraph
    ADD CONSTRAINT uk_paragraph UNIQUE (paragraph_image_id, paragraph_index);


--
-- TOC entry 1906 (class 2606 OID 5000388)
-- Name: uk_row_index; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_row
    ADD CONSTRAINT uk_row_index UNIQUE (row_paragraph_id, row_index);


--
-- TOC entry 1910 (class 2606 OID 5000390)
-- Name: uk_shape_in_group; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_shape
    ADD CONSTRAINT uk_shape_in_group UNIQUE (shape_group_id, shape_index);


--
-- TOC entry 1916 (class 2606 OID 5000392)
-- Name: uk_split; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_split
    ADD CONSTRAINT uk_split UNIQUE (split_shape_id, split_position);


--
-- TOC entry 1920 (class 2606 OID 5000394)
-- Name: uk_username; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_user
    ADD CONSTRAINT uk_username UNIQUE (user_username);


--
-- TOC entry 1924 (class 2606 OID 5000396)
-- Name: uk_word_text; Type: CONSTRAINT; Schema: public; Owner: nlp; Tablespace: 
--

ALTER TABLE ONLY ocr_word
    ADD CONSTRAINT uk_word_text UNIQUE (word_text);


--
-- TOC entry 1911 (class 1259 OID 5000397)
-- Name: idx_split_position; Type: INDEX; Schema: public; Owner: nlp; Tablespace: 
--

CREATE INDEX idx_split_position ON ocr_split USING btree (split_position);


--
-- TOC entry 1912 (class 1259 OID 5000398)
-- Name: idx_split_shape_id; Type: INDEX; Schema: public; Owner: nlp; Tablespace: 
--

CREATE INDEX idx_split_shape_id ON ocr_split USING btree (split_shape_id);


--
-- TOC entry 1938 (class 2620 OID 5000399)
-- Name: trg_user_au; Type: TRIGGER; Schema: public; Owner: nlp
--

CREATE TRIGGER trg_user_au
    AFTER UPDATE ON ocr_user
    FOR EACH ROW
    EXECUTE PROCEDURE trig_login();


--
-- TOC entry 1927 (class 2606 OID 5000400)
-- Name: fk_doc_user; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_document
    ADD CONSTRAINT fk_doc_user FOREIGN KEY (doc_owner_id) REFERENCES ocr_user(user_id);


--
-- TOC entry 1925 (class 2606 OID 5000405)
-- Name: fk_docauthor_author; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_doc_author_map
    ADD CONSTRAINT fk_docauthor_author FOREIGN KEY (docauthor_author_id) REFERENCES ocr_author(author_id);


--
-- TOC entry 1926 (class 2606 OID 5000410)
-- Name: fk_docauthor_doc; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_doc_author_map
    ADD CONSTRAINT fk_docauthor_doc FOREIGN KEY (docauthor_doc_id) REFERENCES ocr_document(doc_id);


--
-- TOC entry 1928 (class 2606 OID 5000415)
-- Name: fk_group_row; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_group
    ADD CONSTRAINT fk_group_row FOREIGN KEY (group_row_id) REFERENCES ocr_row(row_id);


--
-- TOC entry 1929 (class 2606 OID 5000420)
-- Name: fk_image_page; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_image
    ADD CONSTRAINT fk_image_page FOREIGN KEY (image_page_id) REFERENCES ocr_page(page_id);


--
-- TOC entry 1930 (class 2606 OID 5000425)
-- Name: fk_image_status; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_image
    ADD CONSTRAINT fk_image_status FOREIGN KEY (image_imgstatus_id) REFERENCES ocr_image_status(imgstatus_id);


--
-- TOC entry 1931 (class 2606 OID 5000430)
-- Name: fk_image_user; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_image
    ADD CONSTRAINT fk_image_user FOREIGN KEY (image_owner_id) REFERENCES ocr_user(user_id);


--
-- TOC entry 1932 (class 2606 OID 5000435)
-- Name: fk_login_user; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_login
    ADD CONSTRAINT fk_login_user FOREIGN KEY (login_user_id) REFERENCES ocr_user(user_id);


--
-- TOC entry 1933 (class 2606 OID 5000440)
-- Name: fk_page_doc; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_page
    ADD CONSTRAINT fk_page_doc FOREIGN KEY (page_doc_id) REFERENCES ocr_document(doc_id);


--
-- TOC entry 1934 (class 2606 OID 5000445)
-- Name: fk_paragraph_image; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_paragraph
    ADD CONSTRAINT fk_paragraph_image FOREIGN KEY (paragraph_image_id) REFERENCES ocr_image(image_id);


--
-- TOC entry 1935 (class 2606 OID 5000450)
-- Name: fk_row_paragraph; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_row
    ADD CONSTRAINT fk_row_paragraph FOREIGN KEY (row_paragraph_id) REFERENCES ocr_paragraph(paragraph_id);


--
-- TOC entry 1936 (class 2606 OID 5000455)
-- Name: fk_shape_group; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_shape
    ADD CONSTRAINT fk_shape_group FOREIGN KEY (shape_group_id) REFERENCES ocr_group(group_id);


--
-- TOC entry 1937 (class 2606 OID 5000460)
-- Name: fk_split_shape; Type: FK CONSTRAINT; Schema: public; Owner: nlp
--

ALTER TABLE ONLY ocr_split
    ADD CONSTRAINT fk_split_shape FOREIGN KEY (split_shape_id) REFERENCES ocr_shape(shape_id);


--
-- TOC entry 1945 (class 0 OID 0)
-- Dependencies: 6
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2015-02-13 17:25:35

--
-- PostgreSQL database dump complete
--

