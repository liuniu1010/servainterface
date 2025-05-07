curl -sS -X POST "http://172.17.0.1:18080/api/aigamefactory/canceljob" \
     -H "Content-Type: application/json; charset=utf-8" \
     -d '{"job_id":"sZSCxHpiEY"}' \
     -o body.json \
     -D headers.txt \
     -w '%{http_code}\n' \
     > status.txt \
     2> curl.err

