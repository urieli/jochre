jochre_web
==========

Note that the target is not directly usable under Tomcat.
The following library needs to be deleted from WEB-INF/lib and copied instead into Tomcat's lib, in order to ensure that it gets loaded AFTER the other libraries.

* levigo-jbig2-imageio-1.6.1.jar
