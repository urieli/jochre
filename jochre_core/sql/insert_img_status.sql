--
-- PostgreSQL database dump
--

-- Started on 2013-01-10 08:34:42

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = public, pg_catalog;

--
-- TOC entry 1851 (class 0 OID 4995470)
-- Dependencies: 1554
-- Data for Name: ocr_image_status; Type: TABLE DATA; Schema: public; Owner: nlp
--

INSERT INTO ocr_image_status (imgstatus_id, imgstatus_name) VALUES (1, 'Training - new');
INSERT INTO ocr_image_status (imgstatus_id, imgstatus_name) VALUES (2, 'Training - validated');
INSERT INTO ocr_image_status (imgstatus_id, imgstatus_name) VALUES (3, 'Training - hold-out');
INSERT INTO ocr_image_status (imgstatus_id, imgstatus_name) VALUES (4, 'Training - test');
INSERT INTO ocr_image_status (imgstatus_id, imgstatus_name) VALUES (5, 'Auto - new');
INSERT INTO ocr_image_status (imgstatus_id, imgstatus_name) VALUES (6, 'Auto - validated');


-- Completed on 2013-01-10 08:34:42

--
-- PostgreSQL database dump complete
--

