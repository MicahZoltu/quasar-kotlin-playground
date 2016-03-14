package com.zoltu

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.LocalActor
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.kotlin.fiber
import co.paralleluniverse.kotlin.register
import co.paralleluniverse.kotlin.spawn
import co.paralleluniverse.strands.channels.Channels
import co.paralleluniverse.strands.channels.IntChannel

fun doAll() {
	val toEcho = Channels.newIntChannel(0, Channels.OverflowPolicy.BLOCK, true, true)
	val toIncreasing = Channels.newIntChannel(0, Channels.OverflowPolicy.BLOCK, true, true)

	val increasing = fiber { increaser(toIncreasing, toEcho)}
	val echo = fiber { echo(toEcho, toIncreasing)}

	increasing.join()
	echo.join()
}

@Suspendable
fun increaser(inbound: IntChannel, outbound: IntChannel) {
	outbound.send(1)
	loop@ while (true) {
		val receivedValue = inbound.receive()
		when (receivedValue) {
			null, 10 -> {
				outbound.close()
				break@loop
			}
			else -> {
				outbound.send(receivedValue.inc())
			}
		}
	}
}

@Suspendable
fun echo(receiptChannel: IntChannel, responseChannel: IntChannel) {
	loop@ while (true) {
		val receivedValue = receiptChannel.receive()
		when (receivedValue) {
			null -> {
				responseChannel.close()
				break@loop
			}
			else -> {
				println("Echo received ${receivedValue}")
				responseChannel.send(receivedValue)
			}
		}
	}
}

data class Payload(val message: String, val returnAddress: ActorRef<Any?>)
class TerminationSentinel()

class Echoer() : Actor() {
	@Suspendable
	override fun doRun() {
		while (true) {
			receive {
				when (it) {
					is TerminationSentinel -> {
						return@doRun
					}
					is Payload -> {
						println("${it.returnAddress.name} said ${it.message}")
						it.returnAddress.send(Payload("Goodbye.", self()))
					}
					else -> {
						println("wtf")
						return@doRun
					}
				}
			}
		}
	}
}

class Sender(private val recipient: ActorRef<Any?>) : Actor() {
	@Suspendable
	override fun doRun() {
		recipient.send(Payload("Hello", self()))
		receive {
			when (it) {
				is Payload -> {
					println("${it.returnAddress.name} said ${it.message}")
					it.returnAddress.send(TerminationSentinel())
				}
				is TerminationSentinel -> {
					return@doRun
				}
				else -> {
					println("wtf")
					return@doRun
				}
			}
		}
	}
}

fun main(args: Array<String>) {
	val echoer = spawn(register("Bob", Echoer()))
	val sender = spawn(register("Alice", Sender(echoer)))
	LocalActor.join(echoer)
	LocalActor.join(sender)
}
