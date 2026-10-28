package com.example.services.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.util.concurrent.TimeUnit

class SshService {

    /**
     * Executes a command on a remote debug target.
     */
    suspend fun executeCommand(
        command: String,
        authKeyOrPass: String,
        isPassword: Boolean = true,
        host: String = "127.0.0.1",
        user: String = "user"
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            val ssh = SSHClient()
            try {
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                ssh.connect(host)
                
                if (isPassword) {
                    ssh.authPassword(user, authKeyOrPass)
                } else {
                    ssh.authPublickey(user, authKeyOrPass)
                }

                val session = ssh.startSession()
                session.use {
                    val cmd = session.exec(command)
                    cmd.join(15, TimeUnit.SECONDS)
                    
                    val output = cmd.inputStream.bufferedReader().readText()
                    val error = cmd.errorStream.bufferedReader().readText()
                    
                    if (cmd.exitStatus == 0) {
                        Result.success(output.ifEmpty { "Command executed successfully" })
                    } else {
                        Result.failure(Exception("Exit code: ${cmd.exitStatus}. $error"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                try {
                    ssh.disconnect()
                } catch (_: Exception) {}
            }
        }
    }
}
