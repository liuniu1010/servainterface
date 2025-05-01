docker run -d \
-p <serverip>:18080:8080 \
--add-host=mydb:<dbip> \
--name servainterface \
--restart=unless-stopped \
servainterface:0.1
