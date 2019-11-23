"""jochre_search_django URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
  https://docs.djangoproject.com/en/2.1/topics/http/urls/
Examples:
Function views
  1. Add an import:  from my_app import views
  2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
  1. Add an import:  from other_app.views import Home
  2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
  1. Import the include() function: from django.urls import include, path
  2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.urls import path, include
from django.utils import timezone
from django.views.decorators.http import last_modified
from django.views.i18n import JavaScriptCatalog

urlpatterns = [
  path('', include('jochre.urls')),
  path('admin/', admin.site.urls),
  path('accounts/', include('allauth.urls')),
  path('i18n/', include('django.conf.urls.i18n')),
  path('jsi18n/',
     last_modified(lambda req, **kw: last_modified_date)(JavaScriptCatalog.as_view()),
     name='javascript-catalog'),
]

# Attempt to add custom URLs if available.
try:
  urlpatterns = [
    path('', include('custom.urls')),
  ] + urlpatterns
except ModuleNotFoundError:
  pass
