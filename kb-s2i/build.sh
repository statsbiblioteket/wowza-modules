#!/usr/bin/env bash

cd /tmp/src

tar xf /tmp/src/wowza-mediestream-vhost/target/wowza-mediestream-vhost-*-bundle.tar.gz -C /app/conf/
mv /app/conf/wowza-mediestream-vhost-* /app/conf/wowza-mediestream-vhost

