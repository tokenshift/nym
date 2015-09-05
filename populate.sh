#!/bin/sh

if [ -z "$BASE_URL" ]; then
	>&2 echo 'Missing $BASE_URL'
	exit 1
fi

if [ -z "$API_USERNAME" ]; then
	>&2 echo 'Missing $API_USERNAME'
	exit 1
fi

if [ -z "$API_PASSWORD" ]; then
	>&2 echo 'Missing $API_PASSWORD'
	exit 1
fi

urlencode() {
  local string="${1}"
  local strlen=${#string}
  local encoded=""

  for (( pos=0 ; pos<strlen ; pos++ )); do
     c=${string:$pos:1}
     case "$c" in
        [-_.~a-zA-Z0-9] ) o="${c}" ;;
        * )               printf -v o '%%%02x' "'$c"
     esac
     encoded+="${o}"
  done

  echo $encoded
}

tags=()
mode="Tags"
echo "$mode:"
while read -r line; do
	if [ -z "$line" ]; then
		if [ "$mode" = "Tags" ]; then
			mode="Names"
		else
			mode="Tags"
			tags=()
		fi

		>&2 echo "$mode:"
		continue
	fi


	if [ "$mode" = "Tags" ]; then
		tags+=("$line")
	else
		for tag in "${tags[@]}"; do
			echo "PUT /words/`urlencode "$line"`/`urlencode "$tag"`"
			curl -X PUT -u "$API_USERNAME:$API_PASSWORD" "$BASE_URL/words/`urlencode "$line"`/`urlencode "$tag"`" -s -k > /dev/null &
		done
	fi
done