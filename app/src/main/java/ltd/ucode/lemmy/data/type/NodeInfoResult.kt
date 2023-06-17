package ltd.ucode.lemmy.data.type

import ltd.ucode.lemmy.data.type.webfinger.NodeInfo
import ltd.ucode.lemmy.data.type.webfinger.Resource

class NodeInfoResult(val resource: Resource, val nodeInfo: NodeInfo) {
}
