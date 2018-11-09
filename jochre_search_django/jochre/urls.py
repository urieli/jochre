"""jochre_search_django URL Configuration
"""
from django.urls import path
from django.views.generic.base import TemplateView
from jochre.views import search, keyboard, preferences, updateKeyboard, updatePreferences, contents

urlpatterns = [
	path('', search, name='home'),
	path('keyboard', keyboard, name='keyboard'),
	path('updateKeyboard', updateKeyboard, name='updateKeyboard'),
	path('preferences', preferences, name='preferences'),
	path('updatePreferences', updatePreferences, name='updatePreferences'),
	path('contents', contents, name='contents'),
	path('privacy', TemplateView.as_view(template_name='privacy-local.html'), name="privacy"),
]
