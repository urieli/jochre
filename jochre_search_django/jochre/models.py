from __future__ import unicode_literals

from django.db import models
from django.conf import settings


class KeyboardMapping(models.Model):
  user = models.OneToOneField(
    settings.AUTH_USER_MODEL,
    on_delete=models.CASCADE
  )
  mapping = models.TextField()
  enabled = models.BooleanField(default=True)


class Preferences(models.Model):
  user = models.OneToOneField(
    settings.AUTH_USER_MODEL,
    on_delete=models.CASCADE
  )
  docsPerPage = models.IntegerField()
  snippetsPerDoc = models.IntegerField()
  lang = models.CharField(max_length=10, default=settings.DEFAULT_LANG)
