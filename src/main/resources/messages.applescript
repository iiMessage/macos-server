on run {msgText, chatId}
tell application "Messages"
set serviceID to id of 1st service whose service type = iMessage
if exists chat id chatId then
set finalChat to chat id chatId
send msgText to finalChat
else
set chatId to ((characters 12 thru -1 of chatId) as string)
send msgText to buddy chatId of service id serviceID
end if
end tell
end run