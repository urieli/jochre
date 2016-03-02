--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_with_oids = false;

--
-- Name: joc_document; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_document (
    doc_id integer NOT NULL,
    doc_path text NOT NULL
);


--
-- Name: joc_document_doc_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_document_doc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: joc_document_doc_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_document_doc_id_seq OWNED BY joc_document.doc_id;


--
-- Name: joc_font; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_font (
    font_id integer NOT NULL,
    font_code text NOT NULL
);


--
-- Name: joc_font_font_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_font_font_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: joc_font_font_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_font_font_id_seq OWNED BY joc_font.font_id;


--
-- Name: joc_language; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_language (
    language_id integer NOT NULL,
    language_code text NOT NULL
);


--
-- Name: joc_language_language_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_language_language_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: joc_language_language_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_language_language_id_seq OWNED BY joc_language.language_id;


--
-- Name: joc_row; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_row (
    row_id integer NOT NULL,
    row_doc_id integer NOT NULL,
    row_page_index integer NOT NULL,
    row_x integer NOT NULL,
    row_y integer NOT NULL,
    row_width integer NOT NULL,
    row_height integer NOT NULL,
    row_image bytea NOT NULL
);


--
-- Name: joc_row_row_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_row_row_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: joc_row_row_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_row_row_id_seq OWNED BY joc_row.row_id;


--
-- Name: joc_suggestion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_suggestion (
    suggest_id integer NOT NULL,
    suggest_user_id integer NOT NULL,
    suggest_word_id integer NOT NULL,
    suggest_font_id integer NOT NULL,
    suggest_language_id integer NOT NULL,
    suggest_create_date timestamp with time zone DEFAULT now() NOT NULL,
    suggest_text text NOT NULL,
    suggest_previous_text text NOT NULL,
    suggest_applied boolean DEFAULT false NOT NULL,
    suggest_ignore boolean DEFAULT false NOT NULL
);


--
-- Name: joc_suggestion_suggest_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_suggestion_suggest_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: joc_suggestion_suggest_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_suggestion_suggest_id_seq OWNED BY joc_suggestion.suggest_id;


--
-- Name: joc_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_user (
    user_id integer NOT NULL,
    user_username text NOT NULL
);


--
-- Name: joc_user_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_user_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: joc_user_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_user_user_id_seq OWNED BY joc_user.user_id;


--
-- Name: joc_word; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_word (
    word_id integer NOT NULL,
    word_row_id integer NOT NULL,
    word_x integer NOT NULL,
    word_y integer NOT NULL,
    word_width integer NOT NULL,
    word_height integer NOT NULL,
    word_2nd_x integer,
    word_2nd_y integer,
    word_2nd_width integer,
    word_2nd_height integer,
    word_2nd_row_id integer,
    word_initial_guess text NOT NULL,
    word_image bytea NOT NULL
);


--
-- Name: joc_word_word_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_word_word_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: joc_word_word_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_word_word_id_seq OWNED BY joc_word.word_id;


--
-- Name: doc_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_document ALTER COLUMN doc_id SET DEFAULT nextval('joc_document_doc_id_seq'::regclass);


--
-- Name: font_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_font ALTER COLUMN font_id SET DEFAULT nextval('joc_font_font_id_seq'::regclass);


--
-- Name: language_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_language ALTER COLUMN language_id SET DEFAULT nextval('joc_language_language_id_seq'::regclass);


--
-- Name: row_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_row ALTER COLUMN row_id SET DEFAULT nextval('joc_row_row_id_seq'::regclass);


--
-- Name: suggest_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion ALTER COLUMN suggest_id SET DEFAULT nextval('joc_suggestion_suggest_id_seq'::regclass);


--
-- Name: user_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_user ALTER COLUMN user_id SET DEFAULT nextval('joc_user_user_id_seq'::regclass);


--
-- Name: word_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word ALTER COLUMN word_id SET DEFAULT nextval('joc_word_word_id_seq'::regclass);


--
-- Name: pk_document; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_document
    ADD CONSTRAINT pk_document PRIMARY KEY (doc_id);


--
-- Name: pk_font; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_font
    ADD CONSTRAINT pk_font PRIMARY KEY (font_id);


--
-- Name: pk_language; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_language
    ADD CONSTRAINT pk_language PRIMARY KEY (language_id);


--
-- Name: pk_row; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_row
    ADD CONSTRAINT pk_row PRIMARY KEY (row_id);


--
-- Name: pk_suggestion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT pk_suggestion PRIMARY KEY (suggest_id);


--
-- Name: pk_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_user
    ADD CONSTRAINT pk_user PRIMARY KEY (user_id);


--
-- Name: pk_word; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word
    ADD CONSTRAINT pk_word PRIMARY KEY (word_id);


--
-- Name: uk_document; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_document
    ADD CONSTRAINT uk_document UNIQUE (doc_path);


--
-- Name: uk_font; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_font
    ADD CONSTRAINT uk_font UNIQUE (font_code);


--
-- Name: uk_language; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_language
    ADD CONSTRAINT uk_language UNIQUE (language_code);


--
-- Name: uk_row; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_row
    ADD CONSTRAINT uk_row UNIQUE (row_doc_id, row_page_index, row_x, row_y, row_width, row_height);


--
-- Name: uk_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_user
    ADD CONSTRAINT uk_user UNIQUE (user_username);


--
-- Name: uk_word; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word
    ADD CONSTRAINT uk_word UNIQUE (word_row_id, word_x, word_y, word_width, word_height);


--
-- Name: fk_row_document; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_row
    ADD CONSTRAINT fk_row_document FOREIGN KEY (row_doc_id) REFERENCES joc_document(doc_id);


--
-- Name: fk_suggestion_font; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT fk_suggestion_font FOREIGN KEY (suggest_font_id) REFERENCES joc_font(font_id);


--
-- Name: fk_suggestion_language; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT fk_suggestion_language FOREIGN KEY (suggest_language_id) REFERENCES joc_language(language_id);


--
-- Name: fk_suggestion_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT fk_suggestion_user FOREIGN KEY (suggest_user_id) REFERENCES joc_user(user_id);


--
-- Name: fk_suggestion_word; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT fk_suggestion_word FOREIGN KEY (suggest_word_id) REFERENCES joc_word(word_id);


--
-- Name: fk_word_2nd_row; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word
    ADD CONSTRAINT fk_word_2nd_row FOREIGN KEY (word_2nd_row_id) REFERENCES joc_row(row_id);


--
-- Name: fk_word_row; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word
    ADD CONSTRAINT fk_word_row FOREIGN KEY (word_row_id) REFERENCES joc_row(row_id);


--
-- PostgreSQL database dump complete
--

