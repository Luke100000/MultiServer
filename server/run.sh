# Dependencies:
# * Java 17
# * Git
# * velocity.jar and fabric_launcher.jar in server folder

# Kill velocity as it likes to get stuck from past runs
pkill -9 -f "velocity.jar"

# Build and install mods
cd ..
rm build/libs/* || true
rm server/mods/MultiServer.jar || true
./gradlew build || exit
cp "$(find build/libs -maxdepth 1 -type f | head -n 1)" server/mods/MultiServer.jar
cp "$(find build/libs -maxdepth 1 -type f | head -n 1)" server/plugins/MultiServer.jar
cd server || return

# Launch velocity and the server
(
  trap 'kill 0' SIGINT
  java -Xms256M -Xmx256M -XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:+UnlockExperimentalVMOptions -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch -jar velocity.jar &
  java -Xmx5500M -Xms5500M -XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M -jar fabric_launcher.jar nogui
)
