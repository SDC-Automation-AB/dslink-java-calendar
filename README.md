# Calendar DSLink

## Setting up
### Google Calendar
1. Install via DSA /sys/links
2. Start DSLink
3. Go to the Google Cloud Developers API Manager at https://console.cloud.google.com/apis/library
4. Go to the credentials section, click OAuth consent screen, fill out information if not available.
5. Go back to the Credentials tab, click Create, and choose OAuth Client ID.
6. Choose "Other", give it a name.
7. Copy both the client secret, and client ID.
8. Use the "Add Google Calendar" action on the DSLink, give it a description, and fill in client secret and ID.
9. Restart the DSLink.
10. On the root of the DSLink node, your calendar will be added, and there will be two metrics, one for Google Login Code, and one for URL, click the URL.
11. This URL with give you an access code that will bind the DSLink with your Google Calendar, set the code metric to the value you get from the URL.

