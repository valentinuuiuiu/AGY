package com.example.services.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.util.concurrent.TimeUnit

class SshService(
    private val host: String = "100.91.238.121",
    private val user: String = "aryan"
) {

    /**
     * Executes a command on the Pop!_OS machine via SSH.
     * WARNING: Requires valid password or Key authentication.
     */
    suspend fun executeCommand(command: String, authKeyOrPass: String, isPassword: Boolean = true): Result<String> {
        return withContext(Dispatchers.IO) {
            val ssh = SSHClient()
            try {
                // Warning: PromiscuousVerifier disables host key checking (man-in-the-middle risk)
                // Used here for rapid prototyping in the internal Tailscale network.
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                
                ssh.connect(host)
                
                if (isPassword) {
                    ssh.authPassword(user, authKeyOrPass)
                } else {
                    ssh.authPublickey(user, authKeyOrPass) // Pass path to private key
                }

                val session = ssh.startSession()
                session.use {
                    val cmd = session.exec(command)
                    cmd.join(10, TimeUnit.SECONDS)
                    
                    val output = cmd.inputStream.bufferedReader().readText()
                    val error = cmd.errorStream.bufferedReader().readText()
                    
                    if (cmd.exitStatus == 0) {
                        Result.success(output.ifEmpty { "Command executed successfully (no output)" })
                    } else {
                        Result.failure(Exception("Exit code: ${cmd.exitStatus}. Error: $error"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                ssh.disconnect()
            }
        }
    }
}
