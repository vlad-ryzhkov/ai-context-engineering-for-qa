package registration.helpers

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MockServerExtension : BeforeAllCallback, AfterEachCallback {

    override fun beforeAll(context: ExtensionContext) {
        MockRegistrationServer.ensureStarted()
        context.root.getStore(ExtensionContext.Namespace.GLOBAL)
            .getOrComputeIfAbsent("mock-server") {
                ExtensionContext.Store.CloseableResource {
                    MockRegistrationServer.stop()
                }
            }
    }

    override fun afterEach(context: ExtensionContext) {
        MockRegistrationServer.reset()
    }
}
