# Calendar DSLink

## Setting up
### Google Calendar
1. Install via DSA /sys/links
2. Start DSLink
3. Go to the Google Cloud Developers API Manager at https://console.cloud.google.com/apis/library
4. Enable Google Calendar API from the Dashboard page.
5. Go to the credentials section, click OAuth consent screen, fill out information if not available.
6. Go back to the Credentials tab, click Create, and choose OAuth Client ID.
7. Choose "Other", give it a name.
8. Copy both the client secret, and client ID.
9. Use the "Add Google Calendar" action on the DSLink, give it a description, and fill in client secret and ID.
10. Restart the DSLink.
11. On the root of the DSLink node, your calendar will be added, and there will be two metrics, one for Google Login Code, and one for URL, click the URL.
12. This URL with give you an access code that will bind the DSLink with your Google Calendar, set the code metric to the value you get from the URL.
