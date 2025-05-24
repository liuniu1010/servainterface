docker run -d \
-p <serverip>:18080:8080 \
--add-host=mydb:<dbip> \
--name servainterface1 \
--restart=unless-stopped \
servainterface1:0.1
