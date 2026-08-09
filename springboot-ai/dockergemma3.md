# Docker Desktop

Go to settings gear icon in docker desktop and select AI tab.

 - Select - Enable docker model runner
 - Select - Enable host side TCP support
 - Set prot number - 12434, CORS allowed - All

Go to *Model* tab in main window and select Docker Hub.

 - Search for gemma3. Select lattest and download.
 - in cmd prmt - `docker model run ai/gemma3`

# To connect with this model

## Client Machine

```
Test-NetConnection -ComputerName {IP_ADDRESS} -Port 12434
```

- From client machine, use this to check it is allowing the `TcpTestSucceeded : True` If false, then it is not allowed.

## Host Machine 

- Check is it allowing the requests to other machines.

```aiignore
netstat -an | findstr 12434
```

### Fix: Expose Docker Desktop's model runner on all interfaces

This is Docker Desktop's model runner (port 12434 is its default). To make it accessible from other machines on the network:

#### Open Docker Desktop on the remote machine
Go to Settings → Resources → Network (or Features in Development)
Look for an option like "Expose model runner on TCP port" or "Allow LAN access" and enable it
If that option isn't available in your Docker Desktop version, you can use a port proxy on the remote machine (run this in an admin PowerShell):
```
netsh interface portproxy add v4tov4 listenport=12434 listenaddress=0.0.0.0 connectport=12434 connectaddress=127.0.0.1
```
Then allow it through the Windows Firewall:

```
netsh advfirewall firewall add rule name="Docker Model Runner 12434" dir=in action=allow protocol=TCP localport=12434
```

After this, verify it's now listening on all interfaces:

```
netstat -an | findstr 12434
```

### Other steps to follow

Here is a complete, clean, and easy-to-read troubleshooting document you can save for your future reference.
------------------------------
## 📖 Troubleshooting Guide: Connecting Termux to Docker Ollama (Gemma 3)## 📌 Problem Overview
You want to run heavy AI models (like Gemma 3) inside a Docker container on your Windows gaming laptop and access the API externally using Termux on an Android mobile phone over the same home Wi-Fi network.
## 🛠️ Step 1: Verify Base Network Connectivity
Before checking ports, ensure both devices are on the exact same Wi-Fi network subnet and can talk to each other. [1]

1. Find Windows IP: Open Command Prompt (cmd) on Windows and type ipconfig. Locate your IPv4 address (e.g., 192.168.31.229). [2]
2. Ping from Termux: Open Termux and test the basic connection:

ping -c 3 192.168.31.229

* If fails: Check if your router has "AP Isolation" or "Guest Network" enabled, which blocks local device communication. [3]

## 🔒 Step 2: Fix Windows Firewall (Port Blocking)
Windows Defender Firewall automatically blocks incoming external traffic on custom ports like 12434. You must explicitly create an inbound rule. [4, 5, 6]

1. Open the Start menu, type *Windows Defender Firewall* with Advanced Security, and open it.
2. In the left sidebar, click *Inbound Rules*.
3. In the right sidebar, click *New Rule*...
4. Choose Port → Click Next.
5. Select *TCP*, and under Specific local ports, type: 12434 → Click Next.
6. Select Allow the connection → Click Next.
7. Keep Domain, Private, and Public checked → Click Next.
8. Name the rule Docker Ollama API → Click Finish. [7, 8, 9, 10, 11]

## 🐳 Step 3: Configure Docker Environment Variables
Windows System Environment Variables do not pass automatically into isolated Docker containers. You must inject the environment flag when creating the container so Ollama listens to external network interfaces (0.0.0.0) instead of just localhost (127.0.0.1).

1. Stop and remove the old isolated container:

docker stop gemma3

docker rm gemma3

2. Launch a new container with the Environment Flag (-e) and correct port mapping:

docker run -d -p 12434:11434 -e OLLAMA_HOST=0.0.0.0:11434 --name gemma3 ollama/ollama

3. Download and run your model inside the container environment:

docker exec -it gemma3 ollama run gemma3


## 📱 Step 4: Validate and Query from Termux
Once the firewall rule is applied and the Docker container is relaunched, run your validation steps on your phone.

1. Test the port opening using netcat:

nc -zv 192.168.31.229 12434

* Success Output: Connection to 192.168.31.229 12434 port [tcp/*] succeeded!
2. Send a test API prompt (Single JSON response):

curl http://192.168.31 -d '{"model": "gemma3", "prompt": "Why is the sky blue?", "stream": false}'

3. Send a test API prompt (Streaming response - word by word):

curl http://192.168.31 -d '{"model": "gemma3", "prompt": "Hello", "stream": true}'

   
------------------------------
Would you like me to add steps on how to automate starting this container on Windows boot, or create a short shell script for your Termux to make querying easier?

[1] [https://learn.microsoft.com](https://learn.microsoft.com/en-gb/answers/questions/1360645/setting-up-the-wfc-domainless-%28workgroup%29-in-windo)
[2] [https://www.ionos.com](https://www.ionos.com/digitalguide/server/configuration/unidentified-network/)
[3] [https://docs-cybersec.thalesgroup.com](https://docs-cybersec.thalesgroup.com/bundle/cloud-application-security/page/connectivity-report.htm)
[4] [https://www.coursehero.com](https://www.coursehero.com/file/208354166/13-8docx/)
[5] [https://rutbar.medium.com](https://rutbar.medium.com/tryhackme-firewall-fundamentals-cyber-security-101-thm-2ea6b6a86fbd)
[6] [https://www.infosecinstitute.com](https://www.infosecinstitute.com/resources/operating-system-security/how-to-configure-windows-10-firewall/)
[7] [https://learn.microsoft.com](https://learn.microsoft.com/en-us/visualstudio/debugger/configure-the-windows-firewall-for-remote-debugging?view=visualstudio)
[8] [https://wafatech.sa](https://wafatech.sa/blog/windows-server/windows-security/comprehensive-guide-to-windows-server-firewall-management/)
[9] [https://www.nkaccounting.co.za](https://www.nkaccounting.co.za/art-display.php?art=25)
[10] [https://support.secpod.com](https://support.secpod.com/support/solutions/articles/1060000084665-configuring-windows-firewall-to-allow-port-443-ping-requests-and-access-to-saner-secpod-com-for-sea)
[11] [https://support.ucsd.edu](https://support.ucsd.edu/services?id=kb_article_view&sysparm_article=KB0030091)



