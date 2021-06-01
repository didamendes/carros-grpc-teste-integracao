package br.com.zup.edu

import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CarrosEndpointTest(val repository: CarroRepository,
    val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub) {

    @BeforeEach
    internal fun setUp() {
        repository.deleteAll()
    }

    @Test
    internal fun `deve adicionar um novo carro`() {
        // acao
        val response = grpcClient.adicionar(CarrosRequest
                                .newBuilder()
                                .setModelo("Versa")
                                .setPlaca("AAA-1234")
                                .build())

        // validacao
        with(response) {
            assertNotNull(id)
            assertTrue(repository.existsById(id))
        }

    }

    @Test
    internal fun `nao deve adicionar novo carro quando carro com placa ja existente`() {

        val carro = repository.save(Carro(modelo = "Versao", placa = "AAA-1234"))

        // acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarrosRequest.newBuilder()
                    .setModelo("Palio").setPlaca(carro.placa).build()
            )
        }

        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("carro com placa existente", status.description)
        }

    }

    @Test
    internal fun `nao deve adicionar novo carro quando dados de entrada forem invalidos`() {
        // acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarrosRequest.newBuilder().build()
            )
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada invalidos", status.description)
        }
    }

    @Factory
    class Clients {

        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

}