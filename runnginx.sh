docker run -d \
-p 8443:443 \
--name nginx_interface \
--restart=unless-stopped \
-v <sslpathonhost>:/etc/nginx/ssl:ro \
nginx_interface:0.1
