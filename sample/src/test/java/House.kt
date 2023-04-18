import pers.apollokwok.tracer.common.annotations.Tracer

@Tracer.Root
class House : HouseTracer{
    val bedroom = Bedroom()
    val door = Door()
    val livingRoom = LivingRoom()

    override val _House: House = this
}

context (HouseTracer)
class Bedroom{
    private val wifiRouter get() = _WifiRouter_LivingRoom_wifiRouter
}

context (HouseTracer)
class Door

context (HouseTracer)
class LivingRoom{
    val wifiRouter = WifiRouter()

    private val bedroom get() = _Bedroom_House_bedroom
    private val door get() = _Door_House_door
}

context (HouseTracer)
class WifiRouter{
    private val livingRoom get() = _LivingRoom_House_livingRoom
}