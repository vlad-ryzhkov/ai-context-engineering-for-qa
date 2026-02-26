package registration.helpers

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MockServerExtension : BeforeAllCallback, AfterAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        MockRegistrationServer.start()
        System.setProperty("SMS_GATEWAY_URL", "http://localhost:${MockRegistrationServer.port()}")
        MockRegistrationServer.stubSmsGatewaySuccess()

        val server = RegistrationMockServer()
        server.start()
        context.root.getStore(NAMESPACE).put(context.requiredTestClass.name, server)
    }

    override fun afterAll(context: ExtensionContext) {
        context.root.getStore(NAMESPACE)
            .remove(context.requiredTestClass.name, RegistrationMockServer::class.java)
            ?.stop()
        MockRegistrationServer.resetAll()
    }

    companion object {
        private val NAMESPACE = ExtensionContext.Namespace.create(MockServerExtension::class.java)
    }
}
