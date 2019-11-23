from djproxy.views import HttpProxy
from django.conf import settings

class LocalProxy(HttpProxy):
    base_url = settings.JOCHRE_SEARCH_URL