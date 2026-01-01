package org.leeminkan.redis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SimpleClientTest {

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 6379);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            System.out.println("Connected to KanRedis!");

            // 1. SET "user:1" "100"
            System.out.println("Sending SET...");
            out.writeByte(2); // SET
            writeString(out, "user:1");
            writeString(out, "100");

            // Read response (Int Len + Byte status)
            in.readInt();
            byte status = in.readByte();
            System.out.println("SET Status: " + (status == 1 ? "OK" : "FAIL"));

            // 2. CAS "user:1" "100" "150" (Should Succeed)
            System.out.println("Sending CAS (Expected 100 -> 150)...");
            out.writeByte(3); // CAS
            writeString(out, "user:1");
            writeString(out, "100"); // Expected
            writeString(out, "150"); // New

            in.readInt();
            status = in.readByte();
            System.out.println("CAS Status: " + (status == 1 ? "SUCCESS" : "FAIL (Collision)"));

            // 3. CAS "user:1" "100" "200" (Should Fail, because value is now 150)
            System.out.println("Sending CAS (Expected 100 -> 200)...");
            out.writeByte(3); // CAS
            writeString(out, "user:1");
            writeString(out, "100"); // Wrong Expected
            writeString(out, "200"); // New

            in.readInt();
            status = in.readByte();
            System.out.println("CAS Status: " + (status == 1 ? "SUCCESS" : "FAIL (Correct behavior)"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeString(DataOutputStream out, String s) throws Exception {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);
        out.write(b);
    }
}