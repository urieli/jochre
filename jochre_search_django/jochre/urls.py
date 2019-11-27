"""jochre_search_django URL Configuration
"""
from django.urls import path, re_path
from django.views.generic.base import TemplateView, RedirectView
from jochre.views import search, keyboard, preferences, updateKeyboard, updatePreferences, contents, login
from jochre.localProxy import LocalProxy

urlpatterns = [
  path('', search, name='home'),
  path('keyboard', keyboard, name='keyboard'),
  path('updateKeyboard', updateKeyboard, name='updateKeyboard'),
  path('preferences', preferences, name='preferences'),
  path('updatePreferences', updatePreferences, name='updatePreferences'),
  path('contents', contents, name='contents'),
  path('privacy', TemplateView.as_view(template_name='privacy.html'), name="privacy"),
  re_path('^jochre-search/(?P<url>.*)', LocalProxy.as_view(), name='jochre-search'),
  path('accounts/login/', login, name='login'),
]
