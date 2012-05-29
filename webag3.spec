Summary: WebAG3 jabber proxy server
Name: webag3
Version: 1.1
Release: 4
BuildArch: noarch
Packager: Robert W Frank
Source: %{name}-%{version}.tar.gz
License: own
BuildRoot: %{_tmppath}/{%name}-buildroot
Requires: jre >= 1.5

%define INSTALLPATH /opt/webag3

%description
WebAG3 jabber proxy server

%prep
%setup

%install
rm -rf $RPM_BUILD_ROOT
install -m 644 -D etc/webdefaults.xml $RPM_BUILD_ROOT%{INSTALLPATH}/etc/webdefaults.xml
install -m 755 -d $RPM_BUILD_ROOT%{INSTALLPATH}/lib
install -m 644 -t $RPM_BUILD_ROOT%{INSTALLPATH}/lib lib/*.jar
install -m 644 -D www/css/jabber.css $RPM_BUILD_ROOT%{INSTALLPATH}/www/css/jabber.css
install -m 755 -d $RPM_BUILD_ROOT%{INSTALLPATH}/www/jabber
install -m 644 -t $RPM_BUILD_ROOT%{INSTALLPATH}/www/jabber www/jabber/*.jsp
install -m 755 -d $RPM_BUILD_ROOT%{INSTALLPATH}/www/js
install -m 644 -t $RPM_BUILD_ROOT%{INSTALLPATH}/www/js www/js/*.js
install -m 755 -d $RPM_BUILD_ROOT%{INSTALLPATH}/www/WEB-INF/lib
install -m 644 -t $RPM_BUILD_ROOT%{INSTALLPATH}/www/WEB-INF/lib www/WEB-INF/lib/*.jar
install -m 644 -D webag3.jar $RPM_BUILD_ROOT%{INSTALLPATH}/webag3.jar
install -m 644 -D log4j.properties $RPM_BUILD_ROOT%{INSTALLPATH}/log4j.properties
install -m 755 -D webag3.sh $RPM_BUILD_ROOT/etc/init.d/webag3

%clean
rm -rf $RPM_BUILD_ROOT

%post
# $1 == 2 if an existing package is upgraded
# $1 == 1 if it's a fist time install
if [ $1 -gt 1 ]; then
	# just restart in case of upgrade
	/sbin/service webag3 restart
else
	# enable start during boot
	/sbin/chkconfig --add webag3
	# start service
	/sbin/service webag3 start
fi

%preun
# $1 == 1 if an existing package is upgraded
# $1 == 0 if the package is uninstalled
if [ $1 -eq 0 ]; then
	# stop service if it's running
	/sbin/service webag3 status > /dev/null && /sbin/service webag3 stop
	# disable start during boot
	/sbin/chkconfig --del webag3
fi
# do nothing during upgrade

%files
%defattr(-,root,root)
/opt/webag3
/etc/init.d/webag3
%config(noreplace) /opt/webag3/etc/webdefaults.xml

%changelog
* Mon May 21 2012 Robert W Frank <robert.frank@manchester.ac.uk>
- marked webdefaults.xml file as config file
* Thu May 17 2012 Robert W Frank <robert.frank@manchester.ac.uk>
- fixed post and preun scripts to deal with package upgrades properly
* Mon Jan 9 2012 Robert W Frank <robert.frank@manchester.ac.uk>
- Added init script
- Added post install script to register and enable init script
- Added pre uninstall script to stop service and deregister init script
* Fri Jan 6 2012 Robert W Frank <robert.frank@manchester.ac.uk>
- First version
