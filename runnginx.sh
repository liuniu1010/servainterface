docker run -d \
-p 8443:443 \
--name nginx_interface \
--restart=unless-stopped \
-v <conffileonhost>:/etc/nginx/nginx.conf:ro \
-v <sslpathonhost>:/etc/nginx/ssl:ro \
nginx_interface:0.1
