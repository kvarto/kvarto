To reproduce the error:

`./gradlew test --tests *VertxHttpClientTest --info`

Output: 

    > Task :test

    io.kvarto.http.client.impl.VertxHttpClientTest > get success() FAILED
        java.lang.VerifyError: Illegal type at constant pool entry 39 in class io.kvarto.http.client.impl.VertxHttpClient$sendRequest$$inlined$map$1$2
        Exception Details:
          Location:
            io/kvarto/http/client/impl/VertxHttpClient$sendRequest$$inlined$map$1$2.emit(Ljava/lang/Object;Lkotlin/coroutines/Continuation;)Ljava/lang/Object; @44: invokestatic
          Reason:
            Constant pool index 39 is invalid
          Bytecode:
            0x0000000: 002b 2c4e 3a04 0336 052a b400 1219 042c
            0x0000010: 3a06 3a07 3a08 0336 0919 0819 072c 3a0a
            0x0000020: c000 213a 0b3a 0c03 360d 190b b800 273a
            0x0000030: 0e19 0c19 0e2c b900 2903 0059 b800 2fa6
            0x0000040: 0004 b057 b200 35b0                    
          Stackmap Table:
            full_frame(@67,{Object[#2],Object[#5],Object[#66],Object[#66],Object[#5],Integer,Object[#66],Object[#5],Object[#7],Integer,Object[#66],Object[#33],Object[#7],Integer,Object[#35]},{Object[#5]})
    
            Caused by:
            java.lang.VerifyError: Illegal type at constant pool entry 39 in class io.kvarto.http.client.impl.VertxHttpClient$sendRequest$$inlined$map$1$2
            Exception Details:
              Location:
                io/kvarto/http/client/impl/VertxHttpClient$sendRequest$$inlined$map$1$2.emit(Ljava/lang/Object;Lkotlin/coroutines/Continuation;)Ljava/lang/Object; @44: invokestatic
              Reason:
                Constant pool index 39 is invalid
              Bytecode:
                0x0000000: 002b 2c4e 3a04 0336 052a b400 1219 042c
                0x0000010: 3a06 3a07 3a08 0336 0919 0819 072c 3a0a
                0x0000020: c000 213a 0b3a 0c03 360d 190b b800 273a
                0x0000030: 0e19 0c19 0e2c b900 2903 0059 b800 2fa6
                0x0000040: 0004 b057 b200 35b0                    
              Stackmap Table:
                full_frame(@67,{Object[#2],Object[#5],Object[#66],Object[#66],Object[#5],Integer,Object[#66],Object[#5],Object[#7],Integer,Object[#66],Object[#33],Object[#7],Integer,Object[#35]},{Object[#5]})
                at io.kvarto.http.client.impl.VertxHttpClient$sendRequest$$inlined$map$1.collect(SafeCollector.kt:127)
                at io.kvarto.utils.UtilsKt.writeTo(Utils.kt:75)
                at io.kvarto.http.client.impl.VertxHttpClient.sendRequest(VertxHttpClient.kt:36)
                at io.kvarto.http.client.impl.VertxHttpClient$send$2.invokeSuspend(VertxHttpClient.kt:22)
                at io.kvarto.http.client.impl.VertxHttpClient$send$2.invoke(VertxHttpClient.kt)
                at io.kvarto.http.common.RetryUtilsKt.retry(RetryUtils.kt:15)
                at io.kvarto.http.client.impl.VertxHttpClient.send(VertxHttpClient.kt:20)
                at io.kvarto.http.client.impl.VertxHttpClientTest$get success$1.invokeSuspend(VertxHttpClientTest.kt:26)
                at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
                at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:56)
                at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:271)
                at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:79)
                at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:54)
                at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)
                at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking$default(Builders.kt:36)
                at kotlinx.coroutines.BuildersKt.runBlocking$default(Unknown Source)
                at io.kvarto.http.common.TestUtilsKt.testBlocking(TestUtils.kt:8)
                at io.kvarto.http.client.impl.VertxHttpClientTest.get success(VertxHttpClientTest.kt:18)
    
    1 test completed, 1 failed
    Finished generating test XML results (0.001 secs) into: /Users/xap4o/src/kvarto/build/test-results/test
    Generating HTML test report...
    Finished generating test html results (0.003 secs) into: /Users/xap4o/src/kvarto/build/reports/tests/test
    
    > Task :test FAILED

