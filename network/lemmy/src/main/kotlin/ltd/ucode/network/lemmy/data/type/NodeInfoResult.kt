package ltd.ucode.network.lemmy.data.type

import ltd.ucode.network.lemmy.data.type.webfinger.NodeInfo
import ltd.ucode.network.lemmy.data.type.webfinger.Resource

class NodeInfoResult(val resource: Resource, val nodeInfo: NodeInfo) {
}
