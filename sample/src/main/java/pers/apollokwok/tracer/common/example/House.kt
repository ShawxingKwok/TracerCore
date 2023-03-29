package pers.apollokwok.tracer.common.example

import pers.apollokwok.tracer.common.annotations.Tracer
import pers.apollokwok.tracer.common.generated.HouseTracer
import pers.apollokwok.tracer.common.generated._Door_House_door
import pers.apollokwok.tracer.common.generated._WifiRouter_LivingRoom_wifiRouter

@Tracer.Root
class House : HouseTracer{
    val masterBedroom = Bedroom(_House)
    val secondaryBedroom = Bedroom(_House)
    val door = Door()
    val livingRoom = LivingRoom()

    override val _House: House get() = this
}

context (HouseTracer)
class Door

context (HouseTracer)
class LivingRoom{
    val wifiRouter = WifiRouter()
}

context (HouseTracer)
class WifiRouter