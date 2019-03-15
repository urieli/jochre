--
-- PostgreSQL database dump
--

-- Dumped from database version 9.4.3
-- Dumped by pg_dump version 9.4.3
-- Started on 2016-03-10 09:32:25

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 193 (class 3079 OID 11855)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2128 (class 0 OID 0)
-- Dependencies: 193
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_with_oids = false;

--
-- TOC entry 192 (class 1259 OID 1753639)
-- Name: joc_clause; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_clause (
    clause_query_id integer NOT NULL,
    clause_criterion_id smallint NOT NULL,
    clause_text text NOT NULL
);


--
-- TOC entry 191 (class 1259 OID 1753560)
-- Name: joc_criterion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_criterion (
    criterion_id smallint NOT NULL,
    criterion_name text NOT NULL
);


--
-- TOC entry 190 (class 1259 OID 1753558)
-- Name: joc_criterion_criterion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_criterion_criterion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2129 (class 0 OID 0)
-- Dependencies: 190
-- Name: joc_criterion_criterion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_criterion_criterion_id_seq OWNED BY joc_criterion.criterion_id;


--
-- TOC entry 173 (class 1259 OID 1753368)
-- Name: joc_document; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_document (
    doc_id integer NOT NULL,
    doc_path text NOT NULL
);


--
-- TOC entry 172 (class 1259 OID 1753366)
-- Name: joc_document_doc_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_document_doc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2130 (class 0 OID 0)
-- Dependencies: 172
-- Name: joc_document_doc_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_document_doc_id_seq OWNED BY joc_document.doc_id;


--
-- TOC entry 179 (class 1259 OID 1753408)
-- Name: joc_font; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_font (
    font_id smallint NOT NULL,
    font_code text NOT NULL
);


--
-- TOC entry 178 (class 1259 OID 1753406)
-- Name: joc_font_font_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_font_font_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2131 (class 0 OID 0)
-- Dependencies: 178
-- Name: joc_font_font_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_font_font_id_seq OWNED BY joc_font.font_id;


--
-- TOC entry 187 (class 1259 OID 1753523)
-- Name: joc_ip; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_ip (
    ip_id integer NOT NULL,
    ip_address text NOT NULL
);


--
-- TOC entry 186 (class 1259 OID 1753521)
-- Name: joc_ip_ip_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_ip_ip_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2132 (class 0 OID 0)
-- Dependencies: 186
-- Name: joc_ip_ip_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_ip_ip_id_seq OWNED BY joc_ip.ip_id;


--
-- TOC entry 177 (class 1259 OID 1753395)
-- Name: joc_language; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_language (
    language_id smallint NOT NULL,
    language_code text NOT NULL
);


--
-- TOC entry 176 (class 1259 OID 1753393)
-- Name: joc_language_language_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_language_language_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2133 (class 0 OID 0)
-- Dependencies: 176
-- Name: joc_language_language_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_language_language_id_seq OWNED BY joc_language.language_id;


--
-- TOC entry 189 (class 1259 OID 1753541)
-- Name: joc_query; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_query (
    query_id integer NOT NULL,
    query_user_id integer NOT NULL,
    query_ip_id integer NOT NULL,
    query_date timestamp with time zone DEFAULT now() NOT NULL,
    query_results integer NOT NULL
);


--
-- TOC entry 188 (class 1259 OID 1753539)
-- Name: joc_query_query_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_query_query_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2134 (class 0 OID 0)
-- Dependencies: 188
-- Name: joc_query_query_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_query_query_id_seq OWNED BY joc_query.query_id;


--
-- TOC entry 181 (class 1259 OID 1753421)
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
-- TOC entry 180 (class 1259 OID 1753419)
-- Name: joc_row_row_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_row_row_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2135 (class 0 OID 0)
-- Dependencies: 180
-- Name: joc_row_row_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_row_row_id_seq OWNED BY joc_row.row_id;


--
-- TOC entry 185 (class 1259 OID 1753468)
-- Name: joc_suggestion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_suggestion (
    suggest_id integer NOT NULL,
    suggest_user_id integer NOT NULL,
    suggest_word_id integer NOT NULL,
    suggest_font_id smallint NOT NULL,
    suggest_language_id smallint NOT NULL,
    suggest_create_date timestamp with time zone DEFAULT now() NOT NULL,
    suggest_text text NOT NULL,
    suggest_previous_text text NOT NULL,
    suggest_applied boolean DEFAULT false NOT NULL,
    suggest_ignore boolean DEFAULT false NOT NULL,
    suggest_ip_id integer NOT NULL
);


--
-- TOC entry 184 (class 1259 OID 1753466)
-- Name: joc_suggestion_suggest_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_suggestion_suggest_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2136 (class 0 OID 0)
-- Dependencies: 184
-- Name: joc_suggestion_suggest_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_suggestion_suggest_id_seq OWNED BY joc_suggestion.suggest_id;


--
-- TOC entry 175 (class 1259 OID 1753380)
-- Name: joc_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE joc_user (
    user_id integer NOT NULL,
    user_username text NOT NULL
);


--
-- TOC entry 174 (class 1259 OID 1753378)
-- Name: joc_user_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_user_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2137 (class 0 OID 0)
-- Dependencies: 174
-- Name: joc_user_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_user_user_id_seq OWNED BY joc_user.user_id;


--
-- TOC entry 183 (class 1259 OID 1753441)
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
-- TOC entry 182 (class 1259 OID 1753439)
-- Name: joc_word_word_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE joc_word_word_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 2138 (class 0 OID 0)
-- Dependencies: 182
-- Name: joc_word_word_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE joc_word_word_id_seq OWNED BY joc_word.word_id;


--
-- TOC entry 1962 (class 2604 OID 1753571)
-- Name: criterion_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_criterion ALTER COLUMN criterion_id SET DEFAULT nextval('joc_criterion_criterion_id_seq'::regclass);


--
-- TOC entry 1949 (class 2604 OID 1753462)
-- Name: doc_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_document ALTER COLUMN doc_id SET DEFAULT nextval('joc_document_doc_id_seq'::regclass);


--
-- TOC entry 1952 (class 2604 OID 1753582)
-- Name: font_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_font ALTER COLUMN font_id SET DEFAULT nextval('joc_font_font_id_seq'::regclass);


--
-- TOC entry 1959 (class 2604 OID 1753526)
-- Name: ip_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_ip ALTER COLUMN ip_id SET DEFAULT nextval('joc_ip_ip_id_seq'::regclass);


--
-- TOC entry 1951 (class 2604 OID 1753598)
-- Name: language_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_language ALTER COLUMN language_id SET DEFAULT nextval('joc_language_language_id_seq'::regclass);


--
-- TOC entry 1960 (class 2604 OID 1753544)
-- Name: query_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_query ALTER COLUMN query_id SET DEFAULT nextval('joc_query_query_id_seq'::regclass);


--
-- TOC entry 1953 (class 2604 OID 1753437)
-- Name: row_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_row ALTER COLUMN row_id SET DEFAULT nextval('joc_row_row_id_seq'::regclass);


--
-- TOC entry 1958 (class 2604 OID 1753501)
-- Name: suggest_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion ALTER COLUMN suggest_id SET DEFAULT nextval('joc_suggestion_suggest_id_seq'::regclass);


--
-- TOC entry 1950 (class 2604 OID 1753438)
-- Name: user_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_user ALTER COLUMN user_id SET DEFAULT nextval('joc_user_user_id_seq'::regclass);


--
-- TOC entry 1954 (class 2604 OID 1753465)
-- Name: word_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word ALTER COLUMN word_id SET DEFAULT nextval('joc_word_word_id_seq'::regclass);


--
-- TOC entry 2000 (class 2606 OID 1753646)
-- Name: pk_clause; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_clause
    ADD CONSTRAINT pk_clause PRIMARY KEY (clause_query_id, clause_criterion_id);


--
-- TOC entry 1996 (class 2606 OID 1753573)
-- Name: pk_criterion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_criterion
    ADD CONSTRAINT pk_criterion PRIMARY KEY (criterion_id);


--
-- TOC entry 1964 (class 2606 OID 1753376)
-- Name: pk_document; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_document
    ADD CONSTRAINT pk_document PRIMARY KEY (doc_id);


--
-- TOC entry 1976 (class 2606 OID 1753584)
-- Name: pk_font; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_font
    ADD CONSTRAINT pk_font PRIMARY KEY (font_id);


--
-- TOC entry 1990 (class 2606 OID 1753531)
-- Name: pk_ip; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_ip
    ADD CONSTRAINT pk_ip PRIMARY KEY (ip_id);


--
-- TOC entry 1972 (class 2606 OID 1753600)
-- Name: pk_language; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_language
    ADD CONSTRAINT pk_language PRIMARY KEY (language_id);


--
-- TOC entry 1994 (class 2606 OID 1753547)
-- Name: pk_query; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_query
    ADD CONSTRAINT pk_query PRIMARY KEY (query_id);


--
-- TOC entry 1980 (class 2606 OID 1753429)
-- Name: pk_row; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_row
    ADD CONSTRAINT pk_row PRIMARY KEY (row_id);


--
-- TOC entry 1988 (class 2606 OID 1753479)
-- Name: pk_suggestion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT pk_suggestion PRIMARY KEY (suggest_id);


--
-- TOC entry 1968 (class 2606 OID 1753388)
-- Name: pk_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_user
    ADD CONSTRAINT pk_user PRIMARY KEY (user_id);


--
-- TOC entry 1984 (class 2606 OID 1753446)
-- Name: pk_word; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word
    ADD CONSTRAINT pk_word PRIMARY KEY (word_id);


--
-- TOC entry 1998 (class 2606 OID 1753570)
-- Name: uk_criterion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_criterion
    ADD CONSTRAINT uk_criterion UNIQUE (criterion_name);


--
-- TOC entry 1966 (class 2606 OID 1753392)
-- Name: uk_document; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_document
    ADD CONSTRAINT uk_document UNIQUE (doc_path);


--
-- TOC entry 1978 (class 2606 OID 1753418)
-- Name: uk_font; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_font
    ADD CONSTRAINT uk_font UNIQUE (font_code);


--
-- TOC entry 1992 (class 2606 OID 1753533)
-- Name: uk_ip; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_ip
    ADD CONSTRAINT uk_ip UNIQUE (ip_address);


--
-- TOC entry 1974 (class 2606 OID 1753405)
-- Name: uk_language; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_language
    ADD CONSTRAINT uk_language UNIQUE (language_code);


--
-- TOC entry 1982 (class 2606 OID 1753503)
-- Name: uk_row; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_row
    ADD CONSTRAINT uk_row UNIQUE (row_doc_id, row_page_index, row_x, row_y, row_width, row_height);


--
-- TOC entry 1970 (class 2606 OID 1753390)
-- Name: uk_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_user
    ADD CONSTRAINT uk_user UNIQUE (user_username);


--
-- TOC entry 1986 (class 2606 OID 1753505)
-- Name: uk_word; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word
    ADD CONSTRAINT uk_word UNIQUE (word_row_id, word_x, word_y, word_width, word_height);


--
-- TOC entry 2012 (class 2606 OID 1753652)
-- Name: fk_clause_criterion; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_clause
    ADD CONSTRAINT fk_clause_criterion FOREIGN KEY (clause_criterion_id) REFERENCES joc_criterion(criterion_id);


--
-- TOC entry 2011 (class 2606 OID 1753647)
-- Name: fk_clause_query; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_clause
    ADD CONSTRAINT fk_clause_query FOREIGN KEY (clause_query_id) REFERENCES joc_query(query_id);


--
-- TOC entry 2010 (class 2606 OID 1753553)
-- Name: fk_query_ip; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_query
    ADD CONSTRAINT fk_query_ip FOREIGN KEY (query_ip_id) REFERENCES joc_ip(ip_id);


--
-- TOC entry 2009 (class 2606 OID 1753548)
-- Name: fk_query_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_query
    ADD CONSTRAINT fk_query_user FOREIGN KEY (query_user_id) REFERENCES joc_user(user_id);


--
-- TOC entry 2001 (class 2606 OID 1753432)
-- Name: fk_row_document; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_row
    ADD CONSTRAINT fk_row_document FOREIGN KEY (row_doc_id) REFERENCES joc_document(doc_id);


--
-- TOC entry 2007 (class 2606 OID 1753614)
-- Name: fk_suggestion_font; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT fk_suggestion_font FOREIGN KEY (suggest_font_id) REFERENCES joc_font(font_id);


--
-- TOC entry 2006 (class 2606 OID 1753534)
-- Name: fk_suggestion_ip; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT fk_suggestion_ip FOREIGN KEY (suggest_ip_id) REFERENCES joc_ip(ip_id);


--
-- TOC entry 2008 (class 2606 OID 1753626)
-- Name: fk_suggestion_language; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT fk_suggestion_language FOREIGN KEY (suggest_language_id) REFERENCES joc_language(language_id);


--
-- TOC entry 2004 (class 2606 OID 1753480)
-- Name: fk_suggestion_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT fk_suggestion_user FOREIGN KEY (suggest_user_id) REFERENCES joc_user(user_id);


--
-- TOC entry 2005 (class 2606 OID 1753485)
-- Name: fk_suggestion_word; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_suggestion
    ADD CONSTRAINT fk_suggestion_word FOREIGN KEY (suggest_word_id) REFERENCES joc_word(word_id);


--
-- TOC entry 2003 (class 2606 OID 1753455)
-- Name: fk_word_2nd_row; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word
    ADD CONSTRAINT fk_word_2nd_row FOREIGN KEY (word_2nd_row_id) REFERENCES joc_row(row_id);


--
-- TOC entry 2002 (class 2606 OID 1753450)
-- Name: fk_word_row; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY joc_word
    ADD CONSTRAINT fk_word_row FOREIGN KEY (word_row_id) REFERENCES joc_row(row_id);


-- Completed on 2016-03-10 09:32:25

--
-- PostgreSQL database dump complete
--

CREATE TABLE public.joc_field
(
  field_id serial NOT NULL,
  field_name text NOT NULL,
  CONSTRAINT pk_field PRIMARY KEY (field_id),
  CONSTRAINT uk_field UNIQUE (field_name)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.joc_correction
(
  cor_id serial NOT NULL,
  cor_doc_id integer NOT NULL,
  cor_field_id integer NOT NULL,
  cor_user_id integer NOT NULL,
  cor_value text NOT NULL,
  cor_value_before text,
  cor_date timestamp with time zone NOT NULL DEFAULT now(),
  cor_apply_everywhere boolean NOT NULL DEFAULT false,
  cor_ip_id integer NOT NULL,
  cor_ignore boolean NOT NULL DEFAULT false,
  cor_sent boolean NOT NULL DEFAULT false,
  cor_documents text[],
  CONSTRAINT pk_correction PRIMARY KEY (cor_id),
  CONSTRAINT fk_correction_document FOREIGN KEY (cor_doc_id)
      REFERENCES public.joc_document (doc_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_correction_field FOREIGN KEY (cor_field_id)
      REFERENCES public.joc_field (field_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_correction_user FOREIGN KEY (cor_user_id)
      REFERENCES public.joc_user (user_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_correction_ip FOREIGN KEY (cor_ip_id)
      REFERENCES public.joc_ip (ip_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
