# Example of parse of divided file from S3

## How to run

Please add environment like this:
`ENDPOINT=https://host;ACCESS_KEY=keykey;SECRET_KEY=secretsecret;BUCKET_NAME=nameofbucket`


##Example description:

| class         | purposes                                       |
|---------------|------------------------------------------------|
| Shell         | Download parts of file from s3 and stream them |
| SendToS3      | Send parts to s3                               |
| FileSplit     | Divide file to parts                           |
| FileGenerator | create new json file                           |