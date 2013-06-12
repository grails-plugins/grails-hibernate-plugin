#!/bin/sh
export GRAILS_OPTS="-server -noverify -XX:PermSize=192m -XX:MaxPermSize=768m -Xmx3G -Xms512m -Djava.net.preferIPv4Stack=true"
grails -noreloading test-app "$@"
