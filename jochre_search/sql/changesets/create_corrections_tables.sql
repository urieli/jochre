DROP TABLE IF EXISTS public.joc_correction;

DROP TABLE IF EXISTS public.joc_field;

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
