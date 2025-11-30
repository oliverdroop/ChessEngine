#!/bin/bash

CURL='/usr/bin/curl'
#URL=https://g1zhh0aw57.execute-api.eu-west-2.amazonaws.com/default/chess-application-1_2_4-lambda
URL=https://odroop.co.uk:8081/chess
FEN='rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'
FEN_REGEX='(([bknpqrBKNPQR1-8]{1,8})/){7}[bknpqrBKNPQR1-8]{1,8} [bw] (K?Q?k?q?|-) (([a-h][36])|-) [0-9]{1,3} [0-9]{1,4}'
GAME_END_REGEX='(DRAW|STALEMATE|WHITE_VICTORY|BLACK_VICTORY){1}'
ALGEBRAIC_NOTATION_FIELD_REGEX='\"algebraicNotation\":\"[a-h][1-8]x?[a-h][1-8][qrbn]?\"'
ALGEBRAIC_NOTATION_REGEX='[a-h][1-8]x?[a-h][1-8][qrbn]?'
MOVE_HISTORY=()
MOVE_HISTORY_STRING=""
PLAYING=true

generate_post_data()
{
  cat <<EOF
{"depth":4,"fen":"$FEN","moveHistory":[${MOVE_HISTORY_STRING}]}
EOF
}

while [ $PLAYING == true ]
do
	PAYLOAD="$(generate_post_data)"
	RESULT=$($CURL -s -d "$PAYLOAD" -H "Content-Type: application/json" $URL)

	if [[ "$RESULT" =~ $FEN_REGEX ]]; then
		echo "${BASH_REMATCH[0]}"
		FEN="${BASH_REMATCH[0]}"

    if [[ "$RESULT" =~ $ALGEBRAIC_NOTATION_FIELD_REGEX ]]; then
      algebraicNotation="${BASH_REMATCH[0]}"
      if [[ $algebraicNotation =~ $ALGEBRAIC_NOTATION_REGEX ]]; then
        MOVE_HISTORY+=("\"${BASH_REMATCH[0]}\"")
        printf -v joined '%s,' "${MOVE_HISTORY[@]}"
#        echo "${joined%,}"
        MOVE_HISTORY_STRING="${joined%,}"
      fi
		fi

		if [[ "$RESULT" =~ $GAME_END_REGEX ]]; then
			PLAYING=false
			echo $RESULT
		fi
	else
		PLAYING=false
		echo $RESULT
	fi
done