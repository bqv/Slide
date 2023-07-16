package com.github.ghostdogpr.readability4s

import java.net.URL
import scala.collection.JavaConverters._
import scala.util.control.Breaks
import scala.util.control.Breaks._
import scala.util.matching.Regex
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, TextNode}
import org.jsoup.select.Elements
import Readability._

// scalastyle:off cyclomatic.complexity
// scalastyle:off method.length
// scalastyle:off multiple.string.literals
// scalastyle:off magic.number
// scalastyle:off number.of.methods
case class Readability(uri: String, html: String) {

  private var articleTitle: String = ""
  private var articleExcerpt: String = ""
  private var articleByline: String = ""
  private var articleFavicon: String = ""
  private var articleImage: String = ""
  private var flags = FLAG_STRIP_UNLIKELYS | FLAG_WEIGHT_CLASSES | FLAG_CLEAN_CONDITIONALLY

  private val uriObj = new URL(uri)
  private val scheme = uriObj.getProtocol
  private val prePath = scheme + "://" + uriObj.getAuthority
  private val path = uriObj.getPath
  private val pathBase = prePath + path.substring(0, path.lastIndexOf('/') + 1)

  private def flagIsActive(flag: Int): Boolean = {
    (flags & flag) > 0
  }

  private def removeFlag(flag: Int): Unit = {
    flags = flags & ~flag
  }

  /** *
    * grabArticle - Using a variety of metrics (content score, classname, element types), find the content that is
    * most likely to be the stuff a user wants to read. Then return it wrapped up in a div.
    * */
  private def grabArticle(doc: Document): Option[Element] = {
    Option(doc.body).flatMap(page => {

      val pageCacheHtml = page.html

      var isResult = false
      var result: Option[Element] = None

      while (!isResult) {
        val stripUnlikelyCandidates = flagIsActive(FLAG_STRIP_UNLIKELYS)

        // First, node prepping. Trash nodes that look cruddy (like ones with the
        // class name "comment", etc), and turn divs into P tags where they have been
        // used inappropriately (as in, where they contain no other block level elements.)
        var elementsToScore: Seq[Element] = Seq()
        var node = doc.asInstanceOf[Element]

        while (Option(node).isDefined) {
          breakable {
            val matchString = node.className + " " + node.id

            // Check to see if this node is a byline, and remove it if it is.
            if (checkByline(node, matchString)) {
              node = removeAndGetNext(node)
              break
            }

            val tagName = node.tagName.toUpperCase
            // Remove unlikely candidates
            if (stripUnlikelyCandidates) {
              if (REGEXPS.unlikelyCandidates.findFirstIn(matchString).isDefined &&
                REGEXPS.okMaybeItsACandidate.findFirstIn(matchString).isEmpty &&
                tagName != "BODY" && tagName != "A") {
                node = removeAndGetNext(node)
                break
              }
            }

            // Remove DIV, SECTION, and HEADER nodes without any content(e.g. text, image, video, or iframe).
            if ((tagName == "DIV" || tagName == "SECTION" || tagName == "HEADER" || tagName == "H1" || tagName == "H2" ||
              tagName == "H3" || tagName == "H4" || tagName == "H5" || tagName == "H6") &&
              isElementWithoutContent(node)) {
              node = removeAndGetNext(node)
              break
            }

            if (DEFAULT_TAGS_TO_SCORE.indexOf(tagName) != -1) {
              elementsToScore :+= node
            }

            // Turn all divs that don't have children block level elements into p's
            if (tagName == "DIV") {
              // Sites like http://mobile.slate.com encloses each paragraph with a DIV
              // element. DIVs with only a P element inside and no text content can be
              // safely converted into plain P elements to avoid confusing the scoring
              // algorithm with DIVs with are, in practice, paragraphs.
              if (hasSinglePInsideElement(node)) {
                val newNode = node.children.first
                node.replaceWith(newNode)
                node = newNode
                elementsToScore :+= node
              } else if (!hasChildBlockElement(node)) {
                node = node.tagName("P")
                elementsToScore :+= node
              } else {
                // EXPERIMENTAL
                node.textNodes.forEach(childNode => {
                  if (childNode.text.trim.length > 0) {
                    val p = new Element("p")
                    p.text(childNode.text)
                    p.attr("style", "display:inline")
                    p.classNames(Set("readability - styled").asJava)
                    childNode.replaceWith(p)
                  }
                })
              }
            }
            node = getNextNode(node)
          }
        }

        /**
          * Loop through all paragraphs, and assign a score to them based on how content-y they look.
          * Then add their score to their parent node.
          *
          * A score is determined by things like number of commas, class names, etc. Maybe eventually link density.
          **/
        var candidates: Seq[Element] = Seq()
        elementsToScore.foreach(elementToScore => {
          breakable {

            if (Option(elementToScore.parent).isEmpty) break

            // If this paragraph is less than 25 characters, don't even count it.
            val innerText = getInnerText(elementToScore)
            if (innerText.length < 25) break

            // Exclude nodes with no ancestor.
            val ancestors = getNodeAncestors(elementToScore, 3)
            if (ancestors.isEmpty) break

            // Add a point for the paragraph itself as a base.
            var contentScore = 1.0

            // Add points for any commas within this paragraph.
            contentScore += innerText.split(',').length

            // For every 100 characters in this paragraph, add another point. Up to 3 points.
            contentScore += math.min(math.floor(innerText.length / 100.0), 3.0)

            // Initialize and score ancestors.
            var level = 0
            ancestors.foreach(ancestor => {

              if (!ancestor.hasAttr(contentScoreAttribute)) {
                initializeNode(ancestor)
                candidates :+= ancestor
              }

              // Node score divider:
              // - parent:             1 (no division)
              // - grandparent:        2
              // - great grandparent+: ancestor level * 3
              val scoreDivider = if (level == 0) 1 else if (level == 1) 2 else level * 3
              ancestor.attr(contentScoreAttribute, (ancestor.attr(contentScoreAttribute).toFloat + contentScore / scoreDivider.toFloat).toString)
              level += 1
            })
          }
        })

        // After we've calculated scores, loop through all of the possible
        // candidate nodes we found and find the one with the highest score.
        var topCandidates: Seq[Element] = Seq()
        candidates.foreach(candidate => {

          // Scale the final candidates score based on link density. Good content
          // should have a relatively small link density (5% or less) and be mostly
          // unaffected by this operation.
          val candidateScore = candidate.attr(contentScoreAttribute).toFloat * (1 - getLinkDensity(candidate))
          candidate.attr(contentScoreAttribute, candidateScore.toString)

          breakable {
            for (t <- 0 to DEFAULT_N_TOP_CANDIDATES) {
              if (t >= topCandidates.length || candidateScore > topCandidates(t).attr(contentScoreAttribute).toFloat) {
                topCandidates = topCandidates.take(t) ++ Seq(candidate) ++ topCandidates.drop(t)
                if (topCandidates.length > DEFAULT_N_TOP_CANDIDATES) topCandidates = topCandidates.take(DEFAULT_N_TOP_CANDIDATES)
                break
              }
            }
          }
        })

        var topCandidate = topCandidates.headOption
        var neededToCreateTopCandidate = false
        var parentOfTopCandidate: Option[Element] = None

        // If we still have no top candidate, just use the body as a last resort.
        // We also have to copy the body node so it is something we can modify.
        if (topCandidate.isEmpty || topCandidate.get.tagName.toUpperCase == "BODY") {
          // Move all of the page's children into topCandidate
          topCandidate = Some(new Element("DIV"))
          neededToCreateTopCandidate = true
          // Move everything (not just elements, also text nodes etc.) into the container
          // so we even include text directly in the body:
          page.children.forEach(child => topCandidate.get.appendChild(child))

          page.appendChild(topCandidate.get)

          initializeNode(topCandidate.get)
        } else if (topCandidate.isDefined) {
          // Find a better top candidate node if it contains (at least three) nodes which belong to `topCandidates` array
          // and whose scores are quite closed with current `topCandidate` node.
          var alternativeCandidateAncestors: Seq[Seq[Element]] = Seq()
          topCandidates.drop(1).foreach(anotherCandidate => {
            if (anotherCandidate.attr(contentScoreAttribute).toFloat / topCandidate.get.attr(contentScoreAttribute).toFloat >= 0.75) {
              alternativeCandidateAncestors :+= getNodeAncestors(anotherCandidate)
            }
          })

          val MINIMUM_TOPCANDIDATES = 3
          if (alternativeCandidateAncestors.length >= MINIMUM_TOPCANDIDATES) {
            parentOfTopCandidate = topCandidate.map(_.parent)
            breakable {
              while (parentOfTopCandidate.get.tagName.toUpperCase != "BODY") {
                var listsContainingThisAncestor = 0
                var ancestorIndex = 0
                while (ancestorIndex < alternativeCandidateAncestors.length && listsContainingThisAncestor < MINIMUM_TOPCANDIDATES) {
                  listsContainingThisAncestor += (if (alternativeCandidateAncestors(ancestorIndex).contains(parentOfTopCandidate.get)) 1 else 0)
                  ancestorIndex += 1
                }

                if (listsContainingThisAncestor >= MINIMUM_TOPCANDIDATES) {
                  topCandidate = parentOfTopCandidate
                  break
                }
                parentOfTopCandidate = Some(parentOfTopCandidate.get.parent)
              }
            }
          }
          if (!topCandidate.get.hasAttr(contentScoreAttribute)) {
            initializeNode(topCandidate.get)
          }

          // Because of our bonus system, parents of candidates might have scores
          // themselves. They get half of the node. There won't be nodes with higher
          // scores than our topCandidate, but if we see the score going *up* in the first
          // few steps up the tree, that's a decent sign that there might be more content
          // lurking in other places that we want to unify in. The sibling stuff
          // below does some of that - but only if we've looked high enough up the DOM
          // tree.
          parentOfTopCandidate = topCandidate.map(_.parent)
          var lastScore = topCandidate.get.attr(contentScoreAttribute).toFloat
          // The scores shouldn't get too low.
          val scoreThreshold = lastScore / 3
          val Inner = new Breaks
          val Outer = new Breaks
          Outer.breakable {
            while (parentOfTopCandidate.get.tagName.toUpperCase != "BODY") {
              Inner.breakable {
                if (!parentOfTopCandidate.get.hasAttr(contentScoreAttribute)) {
                  parentOfTopCandidate = Some(parentOfTopCandidate.get.parent)
                  Inner.break
                }
                val parentScore = parentOfTopCandidate.get.attr(contentScoreAttribute).toFloat
                if (parentScore < scoreThreshold) Outer.break
                if (parentScore > lastScore) {
                  // Alright! We found a better parent to use.
                  topCandidate = parentOfTopCandidate
                  Outer.break
                }
                lastScore = parentOfTopCandidate.get.attr(contentScoreAttribute).toFloat
                parentOfTopCandidate = Some(parentOfTopCandidate.get.parent)
              }
            }
          }

          // If the top candidate is the only child, use parent instead. This will help sibling
          // joining logic when adjacent content is actually located in parent's sibling node.
          parentOfTopCandidate = topCandidate.map(_.parent)
          while (parentOfTopCandidate.get.tagName.toUpperCase != "BODY" && parentOfTopCandidate.get.children.size == 1) {
            topCandidate = parentOfTopCandidate
            parentOfTopCandidate = topCandidate.map(_.parent)
          }
          if (!topCandidate.get.hasAttr(contentScoreAttribute)) {
            initializeNode(topCandidate.get)
          }
        }

        // Now that we have the top candidate, look through its siblings for content
        // that might also be related. Things like preambles, content split by ads
        // that we removed, etc.
        val articleContent = doc.createElement("DIV")

        val siblingScoreThreshold = math.max(10, topCandidate.get.attr(contentScoreAttribute).toFloat * 0.2)
        // Keep potential top candidate's parent node to try to get text direction of it later.
        parentOfTopCandidate = topCandidate.map(_.parent)
        val siblings = parentOfTopCandidate.get.children

        var s = 0
        val sl = siblings.size
        while (s < sl) {
          val sibling = siblings.get(s)
          var append = false

          if (sibling == topCandidate.get) {
            append = true
          } else {
            var contentBonus = 0.0

            // Give a bonus if sibling nodes and top candidates have the example same classname
            if (sibling.className == topCandidate.get.className && topCandidate.get.className != "") {
              contentBonus += topCandidate.get.attr(contentScoreAttribute).toFloat * 0.2
            }

            if (sibling.hasAttr(contentScoreAttribute) &&
              ((sibling.attr(contentScoreAttribute).toFloat + contentBonus) >= siblingScoreThreshold)) {
              append = true
            } else if (sibling.nodeName.toUpperCase == "P") {
              val linkDensity = getLinkDensity(sibling)
              val nodeContent = getInnerText(sibling)
              val nodeLength = nodeContent.length

              if (nodeLength > 80 && linkDensity < 0.25) {
                append = true
              } else if (nodeLength < 80 && nodeLength > 0 && linkDensity == 0 &&
                "\\.(| $) ".r.findFirstIn(nodeContent).isDefined) {
                append = true
              }
            }
          }

          if (append) {
            if (ALTER_TO_DIV_EXCEPTIONS.indexOf(sibling.nodeName.toUpperCase) == -1) {
              // We have a node that isn't a common block level element, like a form or td tag.
              // Turn it into a div so it doesn't get filtered out later by accident.
              sibling.tagName("DIV")
            }

            articleContent.appendChild(sibling)
          }
          s += 1
        }

        // So we have all of the content that we need. Now we clean it up for presentation.
        prepArticle(articleContent)

        if (neededToCreateTopCandidate) {
          // We already created a fake div thing, and there wouldn't have been any siblings left
          // for the previous loop, so there's no point trying to create a new div, and then
          // move all the children over. Just assign IDs and class names here. No need to append
          // because that already happened anyway.
          topCandidate.get.attr("id", "readability-page-1")
          topCandidate.get.classNames(Set("page").asJava)
        } else {
          val div = doc.createElement("DIV")
          div.attr("id", "readability-page-1")
          div.classNames(Set("page").asJava)
          while (articleContent.children.size > 0) div.appendChild(articleContent.children.first)
          articleContent.appendChild(div)
        }

        // Now that we've gone through the full algorithm, check to see if
        // we got any meaningful content. If we didn't, we may need to re-run
        // grabArticle with different flags set. This gives us a higher likelihood of
        // finding the content, and the sieve approach gives us a higher likelihood of
        // finding the -right- content.
        if (getInnerText(articleContent).length < DEFAULT_WORD_THRESHOLD) {
          page.html(pageCacheHtml)

          if (flagIsActive(FLAG_STRIP_UNLIKELYS)) {
            removeFlag(FLAG_STRIP_UNLIKELYS)
          } else if (flagIsActive(FLAG_WEIGHT_CLASSES)) {
            removeFlag(FLAG_WEIGHT_CLASSES)
            // this flag degrades the results too much
            // } else if (flagIsActive(FLAG_CLEAN_CONDITIONALLY)) {
            // removeFlag(FLAG_CLEAN_CONDITIONALLY)
          } else {
            isResult = true
            result = None
          }
        } else {
          isResult = true
          result = Some(articleContent)
        }
      }
      result
    })
  }

  private def checkByline(node: Element, matchString: String): Boolean = {
    if (articleByline.isEmpty) {
      false
    }
    else {
      if ((node.attr("rel") == "author" || REGEXPS.byline.findFirstIn(matchString).isDefined) && isValidByline(node.text)) {
        articleByline = node.text.trim
        true
      } else {
        false
      }
    }
  }

  /**
    * Get an elements class/id weight. Uses regular expressions to tell if this
    * element looks good or bad.
    **/
  private def getClassWeight(e: Element): Int = {
    if (!flagIsActive(FLAG_WEIGHT_CLASSES)) {
      0
    } else {

      var weight = 0

      // Look for a special classname
      if (e.className.nonEmpty) {
        if (REGEXPS.negative.findFirstIn(e.className).isDefined) weight -= 25
        if (REGEXPS.positive.findFirstIn(e.className).isDefined) weight += 25
      }

      // Look for a special ID
      if (e.id.nonEmpty) {
        if (REGEXPS.negative.findFirstIn(e.id).isDefined) weight -= 25
        if (REGEXPS.positive.findFirstIn(e.id).isDefined) weight += 25
      }

      weight
    }
  }

  /**
    * Initialize a node with the readability object. Also checks the
    * className/id for special names to add to its score.
    **/
  private def initializeNode(node: Element): Unit = {
    val score = node.tagName.toUpperCase match {
      case "DIV" => 5
      case "PRE" | "TD" | "BLOCKQUOTE" => 3
      case "ADDRESS" | "OL" | "UL" | "DL" | "DD" | "DT" | "LI" | "FORM" => -3
      case "H1" | "H2" | "H3" | "H4" | "H5" | "H6" | "TH" => -5
      case _ => 0
    }

    node.attr(contentScoreAttribute, (score + getClassWeight(node)).toString)
  }

  /**
    * Clean out spurious headers from an Element. Checks things like classnames and link density.
    **/
  private def cleanHeaders(e: Element): Unit = {
    for (headerIndex <- 1 to 2) {
      removeNodes(e.getElementsByTag("h" + headerIndex), header => getClassWeight(header) < 0)
    }
  }

  /**
    * Run any post-process modifications to article content as necessary.
    **/
  private def postProcessContent(articleContent: Element): Unit = {
    // Readability cannot open relative uris so we convert them to absolute uris.
    fixRelativeUris(articleContent)

    // Remove IDs and classes.
    cleanIDsAndClasses(articleContent)
  }

  /**
    * Iterates over a NodeList, calls `filterFn` for each node and removes node
    * if function returned `true`.
    *
    * If function is not passed, removes all the nodes in node list.
    */
  private def removeNodes(nodeList: Elements, filterFn: (Element) => Boolean = (_) => true): Unit = {
    nodeList.forEach(node => if (Option(node.parent).isDefined && filterFn(node)) node.remove())
  }

  /**
    * Iterates over a NodeList, and calls _setNodeTag for each node.
    */
  private def replaceNodeTags(nodeList: Elements, newTagName: String): Unit = {
    nodeList.forEach(node => node.tagName(newTagName))
  }

  private def getAllNodesWithTag(node: Element, tagNames: Seq[String]): Elements = {
    node.select(tagNames.mkString(","))
  }

  private def getNodeAncestors(node: Element, maxDepth: Int = 0): Seq[Element] = {
    var i = 0
    var ancestors: Seq[Element] = Seq()
    var currentNode = node
    while (Option(currentNode.parent).isDefined && (maxDepth == 0 || i < maxDepth)) {
      ancestors :+= currentNode.parent
      i += 1
      currentNode = currentNode.parent
    }
    ancestors
  }

  /**
    * Removes the id="" and class="" attribute from every element in the given
    * subtree, except those that match IDS_TO_PRESERVE, CLASSES_TO_PRESERVE and
    * the classesToPreserve array from the options object.
    */
  private def cleanIDsAndClasses(node: Element): Unit = {
    if (!IDS_TO_PRESERVE.contains(node.id)) {
      node.removeAttr("id")
    }

    val className = "\\s+".r.split(node.className)
      .filter(CLASSES_TO_PRESERVE.contains)
      .mkString(" ")

    if (className.nonEmpty) {
      node.classNames(Set(className).asJava)
    } else {
      node.removeAttr("class")
    }

    node.children.forEach(cleanIDsAndClasses)
  }

  /**
    * Converts each <a> and <img> uri in the given element to an absolute URI,
    * ignoring #ref URIs.
    */
  private def fixRelativeUris(articleContent: Element): Unit = {
    articleContent.getElementsByTag("a").forEach(link => {
      val href = link.attr("href")
      if (href.nonEmpty) {
        // Replace links with javascript: URIs with text content, since they won't work after scripts have been removed from the page.
        if (href.indexOf("javascript:") == 0) {
          link.replaceWith(new TextNode(link.text, link.baseUri))
        } else {
          link.attr("href", toAbsoluteURI(href))
        }
      }
    })

    articleContent.getElementsByTag("img").forEach(img => {
      val src = img.attr("src")
      if (src.nonEmpty) {
        img.attr("src", toAbsoluteURI(src))
      }
    })
  }

  private def toAbsoluteURI(uriToFix: String): String = {
    // If this is already an absolute URI, return it.
    if ("^[a-zA-Z][a-zA-Z0-9\\+\\-\\.]*:".r.findFirstIn(uriToFix).isDefined || uriToFix.isEmpty) {
      uriToFix
    }
    // Scheme-rooted relative URI.
    else if (uriToFix.take(2) == "//") {
      scheme + "://" + uriToFix.drop(2)
    }
    // Prepath-rooted relative URI.
    else if (uriToFix(0) == '/') {
      prePath + uriToFix
    }
    // Dotslash relative URI.
    else if (uriToFix.take(2) == "./") {
      pathBase + uriToFix.drop(2)
    }
    // Ignore hash URIs:
    else if (uriToFix(0) == '#') {
      uriToFix
    }
    // Standard relative URI; add entire path. pathBase already includes a trailing "/".
    else {
      pathBase + uriToFix
    }
  }

  /**
    * Get the article title as an H1.
    **/
  private def getArticleTitle(doc: Document): String = {
    var curTitle = doc.title
    var origTitle = doc.title

    // If they had an element with id "title" in their HTML
    if (curTitle.isEmpty) {
      curTitle = Option(doc.getElementsByTag("title").first).map(getInnerText(_)).getOrElse("")
      origTitle = curTitle
    }

    var titleHadHierarchicalSeparators = false

    def wordCount(str: String) = "\\s+".r.split(str).length

    // If there's a separator in the title, first remove the final part
    if (" [\\|\\-\\\\/>»] ".r.findFirstIn(curTitle).isDefined) {
      titleHadHierarchicalSeparators = " [\\\\/>»] ".r.findFirstIn(curTitle).isDefined
      curTitle = "(.*)[\\|\\-\\\\/>»] .*".r.replaceAllIn(origTitle, "$1")

      // If the resulting title is too short (3 words or fewer), remove the first part instead:
      if (wordCount(curTitle) < 3) curTitle = "[^\\|\\-\\\\/>»]*[\\|\\-\\\\/>»](.*)".r.replaceAllIn(origTitle, "$1")
    } else if (curTitle.indexOf(": ") != -1) {
      // Check if we have an heading containing this exact string, so we
      // could assume it's the full title.
      val headings = doc.getElementsByTag("h1").asScala ++ doc.getElementsByTag("h2").asScala
      val matching: Boolean = headings.exists(_.text == curTitle)

      // If we don't, let's extract the title out of the original title string.
      if (!matching) {
        curTitle = origTitle.substring(origTitle.lastIndexOf(':') + 1)

        // If the title is now too short, try the first colon instead:
        if (wordCount(curTitle) < 3) curTitle = origTitle.substring(origTitle.indexOf(':') + 1)
      }
    } else if (curTitle.length > 150 || curTitle.length < 15) {
      val hOnes = doc.getElementsByTag("h1")

      if (hOnes.size == 1) curTitle = getInnerText(hOnes.first)
    }

    curTitle = curTitle.trim
    // If we now have 4 words or fewer as our title, and either no 'hierarchical' separators (\, /, > or »)
    // were found in the original title or we decreased the number of words by more than 1 word, use the original title.
    val curTitleWordCount = wordCount(curTitle)
    if (curTitleWordCount <= 4 &&
      (!titleHadHierarchicalSeparators ||
        curTitleWordCount != wordCount("[\\|\\-\\\\/>»]+".r.replaceAllIn(origTitle, "")) - 1)) {
      curTitle = origTitle
    }

    curTitle
  }

  /**
    * Prepare the HTML document for readability to scrape it.
    * This includes things like stripping javascript, CSS, and handling terrible markup.
    **/
  private def prepDocument(doc: Document): Unit = {
    // Remove all style tags in head
    removeNodes(doc.getElementsByTag("style"))

    Option(doc.body).foreach(body => replaceBrs(body))

    replaceNodeTags(doc.getElementsByTag("font"), "SPAN")
  }

  /**
    * Finds the next element, starting from the given node, and ignoring
    * whitespace in between. If the given node is an element, the same node is
    * returned.
    */
  private def nextElement(node: Element): Element = {
    var next = node
    while (Option(next).isDefined && REGEXPS.whitespace.findFirstIn(next.text).isDefined) {
      next = next.nextElementSibling
    }
    next
  }

  /**
    * Replaces 2 or more successive <br> elements with a single <p>.
    * Whitespace between <br> elements are ignored. For example:
    * <div>foo<br>bar<br> <br><br>abc</div>
    * will become:
    * <div>foo<br>bar<p>abc</p></div>
    */
  private def replaceBrs(elem: Element) {
    val brTagName = "BR"
    elem.getElementsByTag(brTagName).forEach(br => {
      var next = br.nextElementSibling

      // Whether 2 or more <br> elements have been found and replaced with a <p> block.
      var replaced = false

      // If we find a <br> chain, remove the <br>s until we hit another element
      // or non-whitespace. This leaves behind the first <br> in the chain
      // (which will be replaced with a <p> later).
      next = nextElement(next)
      while (Option(next).isDefined && (next.tagName.toUpperCase == brTagName)) {
        replaced = true
        val brSibling = next.nextElementSibling
        next.remove()
        next = nextElement(brSibling)
      }

      // If we removed a <br> chain, replace the remaining <br> with a <p>. Add
      // all sibling nodes as children of the <p> until we hit another <br> chain.
      if (replaced) {
        val p = new Element("p")
        br.replaceWith(p)

        next = p.nextElementSibling
        var stop = false
        while (Option(next).isDefined && !stop) {
          // If we've hit another <br><br>, we're done adding children to this <p>.
          if (next.tagName.toUpperCase == brTagName) {
            val nextElem = nextElement(next)
            if (Option(nextElem).isDefined && nextElem.tagName.toUpperCase == brTagName) stop = true
          }

          if (!stop) {
            // Otherwise, make this node a child of the new <p>.
            val sibling = next.nextElementSibling
            p.appendChild(next)
            next = sibling
          }
        }
      }
    })
  }

  /**
    * Prepare the article node for display. Clean out any inline styles,
    * iframes, forms, strip extraneous <p> tags, etc.
    **/
  private def prepArticle(articleContent: Element): Unit = {
    cleanStyles(articleContent)

    // Check for data tables before we continue, to avoid removing items in
    // those tables, which will often be isolated even though they're
    // visually linked to other content-ful elements (text, images, etc.).
    markDataTables(articleContent)

    // Clean out junk from the article content
    cleanConditionally(articleContent, "form")
    cleanConditionally(articleContent, "fieldset")
    clean(articleContent, "object")
    clean(articleContent, "embed")
    clean(articleContent, "h1")
    clean(articleContent, "footer")

    // Clean out elements have "share" in their id/class combinations from final top candidates,
    // which means we don't remove the top candidates even they have "share".
    articleContent.children.forEach(topCandidate => cleanMatchedNodes(topCandidate, "share".r))

    // If there is only one h2 and its text content substantially equals article title,
    // they are probably using it as a header and not a subheader,
    // so remove it since we already extract the title separately.
    val h2 = articleContent.getElementsByTag("h2")
    if (h2.size == 1) {
      val lengthSimilarRate = (h2.first.text.length - articleTitle.length) / articleTitle.length.toFloat
      if (math.abs(lengthSimilarRate) < 0.5) {
        var titlesMatch = false
        if (lengthSimilarRate > 0) {
          titlesMatch = h2.first.text.contains(articleTitle)
        } else {
          titlesMatch = articleTitle.contains(h2.first.text)
        }
        if (titlesMatch) {
          clean(articleContent, "h2")
        }
      }
    }

    clean(articleContent, "iframe")
    clean(articleContent, "input")
    clean(articleContent, "textarea")
    clean(articleContent, "select")
    clean(articleContent, "button")
    cleanHeaders(articleContent)

    // Do these last as the previous stuff may have removed junk
    // that will affect these
    cleanConditionally(articleContent, "table")
    cleanConditionally(articleContent, "ul")
    cleanConditionally(articleContent, "div")

    // Remove extra paragraphs
    removeNodes(articleContent.getElementsByTag("p"), (paragraph) => {
      val imgCount = paragraph.getElementsByTag("img").size
      val embedCount = paragraph.getElementsByTag("embed").size
      val objectCount = paragraph.getElementsByTag("object").size
      // At this point, nasty iframes have been removed, only remain embedded video ones.
      val iframeCount = paragraph.getElementsByTag("iframe").size
      val totalCount = imgCount + embedCount + objectCount + iframeCount

      totalCount == 0 && getInnerText(paragraph, normalizeSpaces = false).isEmpty
    })

    getAllNodesWithTag(articleContent, Seq("br")).forEach(br => {
      val next = nextElement(br.nextElementSibling)
      if (Option(next).isDefined && next.tagName.toUpperCase == "P") br.remove()
    })
  }

  private def removeAndGetNext(node: Element): Element = {
    val nextNode = getNextNode(node, ignoreSelfAndKids = true)
    node.remove()
    nextNode
  }

  /**
    * Traverse the DOM from node to node, starting at the node passed in.
    * Pass true for the second parameter to indicate this node itself
    * (and its kids) are going away, and we want the next node over.
    *
    * Calling this in a loop will traverse the DOM depth-first.
    */
  private def getNextNode(node: Element, ignoreSelfAndKids: Boolean = false): Element = {
    // First check for kids if those aren't being ignored
    if (!ignoreSelfAndKids && node.children.size > 0) {
      node.children.first
    }
    // Then for siblings...
    else if (Option(node.nextElementSibling).isDefined) {
      node.nextElementSibling
    }
    // And finally, move up the parent chain *and* find a sibling
    // (because this is depth-first traversal, we will have already
    // seen the parent nodes themselves).
    else {
      var tempNode = node
      do {
        tempNode = tempNode.parent
      } while (Option(tempNode).isDefined && Option(tempNode.nextElementSibling).isEmpty)
      if (Option(tempNode).isDefined) tempNode.nextElementSibling else tempNode
    }
  }

  /**
    * Removes script tags from the document.
    **/
  private def removeScripts(doc: Document): Unit = {
    removeNodes(doc.getElementsByTag("script"), scriptNode => {
      scriptNode.`val`("")
      scriptNode.removeAttr("src")
      true
    })
    removeNodes(doc.getElementsByTag("noscript"))
  }

  /**
    * Check if this node has only whitespace and a single P element
    * Returns false if the DIV node contains non-empty text nodes
    * or if it contains no P or more than 1 element.
    **/
  private def hasSinglePInsideElement(element: Element): Boolean = {
    // There should be exactly 1 element child which is a P:
    if (element.children.size != 1 || element.children.first.tagName.toUpperCase != "P") {
      false
    }
    // And there should be no text nodes with real content
    else {
      element.textNodes.asScala.exists(node => REGEXPS.hasContent.findFirstIn(node.text).isDefined)
    }
  }

  private def isElementWithoutContent(node: Element): Boolean = {
    node.text.trim.length == 0 &&
      (node.children.size == 0 ||
        node.children.size == node.getElementsByTag("br").size + node.getElementsByTag("hr").size)
  }

  /**
    * Determine whether element has any children block level elements.
    */
  private def hasChildBlockElement(element: Element): Boolean = {
    element.children.asScala.exists(node => {
      DIV_TO_P_ELEMS.indexOf(node.tagName.toUpperCase) != -1 || hasChildBlockElement(node)
    })
  }

  /**
    * Get the inner text of a node - cross browser compatibly.
    * This also strips out any excess whitespace to be found.
    **/
  private def getInnerText(e: Element, normalizeSpaces: Boolean = true): String = {
    val textContent = e.text.trim
    if (normalizeSpaces) REGEXPS.normalize.replaceAllIn(textContent, " ") else textContent
  }

  /**
    * Get the number of times a string s appears in the node e.
    **/
  private def getCharCount(e: Element, s: String): Int = getInnerText(e).split(s).length - 1

  /**
    * Remove the style attribute on every e and under.
    **/
  private def cleanStyles(e: Element): Unit = {
    if (Option(e).isDefined && e.tagName.toLowerCase != "svg") {

      if (e.className != "readability - styled") {
        // Remove `style` and deprecated presentational attributes
        PRESENTATIONAL_ATTRIBUTES.foreach(e.removeAttr)

        if (DEPRECATED_SIZE_ATTRIBUTE_ELEMS.indexOf(e.tagName.toUpperCase) != -1) {
          e.removeAttr("width")
          e.removeAttr("height")
        }
      }

      var cur = e.children.first
      while (Option(cur).isDefined) {
        cleanStyles(cur)
        cur = cur.nextElementSibling
      }
    }
  }

  /**
    * Get the density of links as a percentage of the content
    * This is the amount of text that is inside a link divided by the total text in the node.
    **/
  private def getLinkDensity(element: Element): Float = {
    val textLength = getInnerText(element).length
    if (textLength == 0) {
      0
    } else {
      var linkLength = 0
      element.getElementsByTag("a").forEach(linkNode => linkLength += getInnerText(linkNode).length)
      linkLength / textLength.toFloat
    }
  }

  /**
    * Clean a node of all elements of type "tag".
    * (Unless it's a youtube/vimeo video. People love movies.)
    **/
  private def clean(e: Element, tag: String): Unit = {
    val isEmbed = Set("object", "embed", "iframe").contains(tag)

    removeNodes(e.getElementsByTag(tag), (element) => {
      // Allow youtube and vimeo videos through as people usually want to see those.
      if (isEmbed) {
        val attributeValues = element.attributes.dataset.values.asScala.mkString("|")
        // First, check the elements attributes to see if any of them contain youtube or vimeo
        REGEXPS.videos.findFirstIn(attributeValues).isEmpty && REGEXPS.videos.findFirstIn(element.html).isEmpty
      } else {
        true
      }
    })
  }

  /**
    * Check if a given node has one of its ancestor tag name matching the
    * provided one.
    */
  private def hasAncestorTag(node: Element, tagName: String, maxDepth: Int = 3, filterFn: (Element) => Boolean = (_) => true) = {
    val tagNameUpper = tagName.toUpperCase
    var depth = 0
    var currentNode = node
    var result: Option[Boolean] = None
    while (Option(currentNode.parentNode).isDefined && result.isEmpty) {
      if (maxDepth > 0 && depth > maxDepth) {
        result = Some(false)
      }
      else if (currentNode.parent.tagName.toUpperCase == tagNameUpper && filterFn(currentNode.parent)) {
        result = Some(true)
      }
      else {
        currentNode = currentNode.parent
        depth += 1
      }
    }
    result.getOrElse(false)
  }

  /**
    * Return an object indicating how many rows and columns this table has.
    */
  private def getRowAndColumnCount(table: Element): (Int, Int) = {
    var rows = 0
    var columns = 0
    table.getElementsByTag("tr").forEach(tr => {
      val rowspan = Option(tr.attr("rowspan")).map(r => r.toInt)
      rows += rowspan.getOrElse(1)

      // Now look for column-related info
      var columnsInThisRow = 0
      tr.getElementsByTag("td").forEach(cell => {
        val colspan = Option(cell.attr("colspan")).map(c => c.toInt)
        columnsInThisRow += colspan.getOrElse(1)
      })
      columns = math.max(columns, columnsInThisRow)
    })
    (rows, columns)
  }

  /**
    * Look for 'data' (as opposed to 'layout') tables, for which we use
    * similar checks as
    * https://dxr.mozilla.org/mozilla-central/rev/71224049c0b52ab190564d3ea0eab089a159a4cf/accessible/html/HTMLTableAccessible.cpp#920
    */
  private def markDataTables(root: Element): Unit = {
    val tables = root.getElementsByTag("table")
    tables.forEach(table => {
      breakable {
        val role = table.attr("role")
        if (role == "presentation") {
          table.attr(dataTableAttribute, false)
          break
        }
        val datatable = table.attr("datatable")
        if (datatable == "0") {
          table.attr(dataTableAttribute, false)
          break
        }
        val summary = table.attr("summary")
        if (summary.nonEmpty) {
          table.attr(dataTableAttribute, true)
          break
        }

        val caption = table.getElementsByTag("caption").first
        if (Option(caption).isDefined && caption.childNodes.size > 0) {
          table.attr(dataTableAttribute, true)
          break
        }

        // If the table has a descendant with any of these tags, consider a data table:
        val dataTableDescendants = Seq("col", "colgroup", "tfoot", "thead", "th")

        val descendantExists = (tag: String) => Option(table.getElementsByTag(tag).first).isDefined

        if (dataTableDescendants.exists(descendantExists)) {
          table.attr(dataTableAttribute, true)
          break
        }

        // Nested tables indicate a layout table:
        if (Option(table.getElementsByTag("table").first).isDefined) {
          table.attr(dataTableAttribute, false)
          break
        }

        val (rows, columns) = getRowAndColumnCount(table)
        if (rows >= 10 || columns > 4) {
          table.attr(dataTableAttribute, true)
          break
        }
        // Now just go by size entirely:
        table.attr(dataTableAttribute, rows * columns > 10)
      }
    })
  }

  /**
    * Clean an element of all tags of type "tag" if they look fishy.
    * "Fishy" is an algorithm based on content length, classnames, link density, number of images & embeds, etc.
    **/
  private def cleanConditionally(e: Element, tag: String): Unit = {
    if (flagIsActive(FLAG_CLEAN_CONDITIONALLY)) {
      val isList = tag == "ul" || tag == "ol"

      // Gather counts for other typical elements embedded within.
      // Traverse backwards so we can remove nodes at the same time
      // without effecting the traversal.
      removeNodes(e.getElementsByTag(tag), (node) => {
        // First check if we're in a data table, in which case don't remove us.
        val isDataTable = (t: Element) => t.attr(dataTableAttribute).toUpperCase == "TRUE"

        if (hasAncestorTag(node, "table", -1, isDataTable)) {
          false
        } else {
          val weight = getClassWeight(node)

          if (weight < 0) {
            true
          } else {

            if (getCharCount(node, ",") < 10) {
              // If there are not very many commas, and the number of
              // non-paragraph elements is more than paragraphs or other
              // ominous signs, remove the element.
              val p = node.getElementsByTag("p").size
              val img = node.getElementsByTag("img").size
              val li = node.getElementsByTag("li").size - 100
              val input = node.getElementsByTag("input").size

              var embedCount = 0
              node.getElementsByTag("embed").forEach(embed =>
                if (REGEXPS.videos.findFirstIn(embed.attr("src")).isEmpty) embedCount += 1
              )

              val linkDensity = getLinkDensity(node)
              val contentLength = getInnerText(node).length

              val haveToRemove =
                (img > 1 && p / img < 0.5 && !hasAncestorTag(node, "figure")) ||
                  (!isList && li > p) ||
                  (input > Math.floor(p / 3)) ||
                  (!isList && contentLength < 25 && (img == 0 || img > 2) && !hasAncestorTag(node, "figure")) ||
                  (!isList && weight < 25 && linkDensity > 0.2) ||
                  (weight >= 25 && linkDensity > 0.5) ||
                  ((embedCount == 1 && contentLength < 75) || embedCount > 1)
              haveToRemove
            } else {
              false
            }
          }
        }
      })
    }
  }


  /**
    * Clean out elements whose id/class combinations match specific string.
    **/
  private def cleanMatchedNodes(e: Element, regex: Regex): Unit = {
    val endOfSearchMarkerNode = getNextNode(e, ignoreSelfAndKids = true)
    var next = getNextNode(e)
    while (Option(next).isDefined && next != endOfSearchMarkerNode) {
      if (regex.findFirstIn(next.className + " " + next.id).isDefined) {
        next = removeAndGetNext(next)
      } else {
        next = getNextNode(next)
      }
    }
  }

  /**
    * Check whether the input string could be a byline.
    * This verifies that the input is a string, and that the length
    * is less than 100 chars.
    */
  private def isValidByline(byline: String): Boolean = {
    val trimmed = byline.trim
    (trimmed.length > 0) && (trimmed.length < 100)
  }

  /**
    * Attempts to get excerpt and byline metadata for the article.
    */
  private def grabArticleMetadata(doc: Document): Unit = {
    var values: Map[String, String] = Map()

    // Match "description", or Twitter's "twitter:description" (Cards)
    // in name attribute.
    val namePattern = "(?i)^\\s*((twitter)\\s*:\\s*)?(description|title)\\s*$".r

    // Match Facebook's Open Graph title & description properties.
    val propertyPattern = "(?i)^\\s*og\\s*:\\s*(description|title)\\s*$".r

    // Find description tags.
    doc.getElementsByTag("meta").forEach(element => {
      val elementName = element.attr("name")
      val elementProperty = element.attr("property")

      if (Set(elementName, elementProperty).contains("author")) {
        articleByline = element.attr("content")
      } else {

        var name = ""
        if (namePattern.findFirstIn(elementName).isDefined) {
          name = elementName
        } else if (propertyPattern.findFirstIn(elementProperty).isDefined) {
          name = elementProperty
        }

        if (name.nonEmpty) {
          val content = element.attr("content")
          if (Option(content).isDefined) {
            // Convert to lowercase and remove any whitespace so we can match below.
            name = "\\s".r.replaceAllIn(name.toLowerCase, "")
            values += name -> content.trim
          }
        }
      }
    })

    articleExcerpt = values.getOrElse("og:description",
      values.getOrElse("twitter:description",
        values.getOrElse("description", ""))) // meta description is usually less accurate than og or twitter

    articleTitle = getArticleTitle(doc)
    if (articleTitle.isEmpty) {
      articleTitle = values.getOrElse("og:title",
        values.getOrElse("twitter:title", ""))
    }

    articleFavicon = extractFaviconUrl(doc)
    articleImage = extractImageUrl(doc)
  }

  // remove more than two spaces or newlines
  private def innerTrim(str: String): String = {
    var previousSpace = false
    str.map(c =>
      if (c == ' ' || c.toInt == 9 || c == '\n') {
        previousSpace = true
        ""
      } else {
        val space = if (previousSpace) " " else ""
        previousSpace = false
        space + c
      }
    ).mkString.trim
  }


  private def extractFaviconUrl(doc: Document): String = {
    var faviconUrl = doc.select("head link[rel=\"shortcut icon\"]").attr("href").trim
    if (faviconUrl.isEmpty) faviconUrl = doc.select("head link[rel=icon]").attr("href").trim
    toAbsoluteURI(faviconUrl)
  }

  private def extractImageUrl(doc: Document): String = {
    var imageUrl = doc.select("head meta[property=og:image:secure_url]").attr("content")
    if (imageUrl.isEmpty) {
      imageUrl = doc.select("head meta[property=og:image:url]").attr("content")
    }
    if (imageUrl.isEmpty) {
      imageUrl = doc.select("head meta[property=og:image]").attr("content")
    }
    if (imageUrl.isEmpty) {
      imageUrl = doc.select("head meta[name=twitter:image]").attr("content")
    }
    if (imageUrl.isEmpty) {
      imageUrl = doc.select("link[rel=image_src]").attr("href")
    }
    if (imageUrl.isEmpty) {
      imageUrl = doc.select("head meta[name=thumbnail]").attr("content")
    }

    toAbsoluteURI(imageUrl)
  }

  /**
    * Runs readability.
    *
    * Workflow:
    *  1. Prep the document by removing script tags, css, etc.
    *  2. Build readability's DOM tree.
    *  3. Grab the article content from the current dom tree.
    *  4. Replace the current DOM tree with the new one.
    *  5. Read peacefully.
    **/
  def parse(): Option[Article] = {
    try {
      Option(Jsoup.parse(html)).flatMap(doc => {

        // Remove script tags from the document.
        removeScripts(doc)

        prepDocument(doc)

        grabArticleMetadata(doc)

        grabArticle(doc).map(articleContent => {

          postProcessContent(articleContent)

          // If we haven't found an excerpt in the article's metadata, use the article's
          // first paragraph as the excerpt. This is used for displaying a preview of
          // the article's content.
          if (articleExcerpt.isEmpty) {
            val paragraphs = articleContent.getElementsByTag("p")
            if (paragraphs.size > 0) {
              articleExcerpt = paragraphs.first.text.trim
            }
          }

          val textContent = articleContent.text

          if (articleImage.isEmpty) {
            // if we could not find any image in the metadata, take the first from the article content
            articleImage = articleContent.getElementsByTag("img").attr("src")
          }

          Article(uri,
            innerTrim(articleTitle),
            innerTrim(articleByline),
            articleContent.html,
            textContent,
            textContent.length,
            innerTrim(articleExcerpt),
            articleFavicon,
            articleImage)
        })
      })
    } catch {
      case _: Exception => None
    }
  }
}

object Readability {

  // All of the regular expressions in use within readability.
  // Defined up here so we don't instantiate them repeatedly in loops.
  private object REGEXPS {
    val unlikelyCandidates: Regex =
      """(?i)banner|breadcrumbs|combx|comment|community|cover-wrap|disqus|extra|foot|header|
                                   legends|menu|modal|related|remark|replies|rss|shoutbox|sidebar|skyscraper|social|
                                   sponsor|supplemental|ad-break|agegate|pagination|pager|popup|yom-remote""".r
    val okMaybeItsACandidate: Regex = "(?i)and|article|body|column|main|shadow".r
    val positive: Regex = "(?i)article|body|content|entry|hentry|h-entry|main|page|pagination|post|text|blog|story".r
    val negative: Regex =
      """(?i)hidden|^hid$| hid$| hid |^hid |banner|combx|comment|com-|contact|foot|footer|footnote|
         masthead|media|meta|modal|outbrain|promo|related|scroll|share|shoutbox|sidebar|skyscraper|sponsor|shopping|
         tags|tool|widget""".r
    val extraneous: Regex = "(?i)print|archive|comment|discuss|e[\\-]?mail|share|reply|all|login|sign|single|utility".r
    val byline: Regex = "(?i)byline|author|dateline|writtenby|p-author".r
    val replaceFonts: Regex = "(?i)<(\\/?)font[^>]*>".r
    val normalize: Regex = "\\s{2,}".r
    val videos: Regex = "(?i)\\/\\/(www\\.)?(dailymotion|youtube|youtube-nocookie|player\\.vimeo)\\.com".r
    val nextLink: Regex = "(?i)(next|weiter|continue|>([^\\|]|$)|»([^\\|]|$))".r
    val prevLink: Regex = "(?i)(prev|earl|old|new|<|«)".r
    val whitespace: Regex = "^\\s*$".r
    val hasContent: Regex = "\\S$".r
  }

  // The number of top candidates to consider when analysing how
  // tight the competition is among candidates.
  private val DEFAULT_N_TOP_CANDIDATES = 5

  // The default number of words an article must have in order to return a result
  private val DEFAULT_WORD_THRESHOLD = 500

  private val PRESENTATIONAL_ATTRIBUTES = Seq(
    "align", "background", "bgcolor", "border", "cellpadding", "cellspacing", "frame", "hspace", "rules", "style", "valign", "vspace")

  private val DEPRECATED_SIZE_ATTRIBUTE_ELEMS = Seq("TABLE", "TH", "TD", "HR", "PRE")

  private val DEFAULT_TAGS_TO_SCORE = "section,h2,h3,h4,h5,h6,p,td,pre".toUpperCase.split(",")

  private val DIV_TO_P_ELEMS = Seq("A", "BLOCKQUOTE", "DL", "DIV", "IMG", "OL", "P", "PRE", "TABLE", "UL", "SELECT")

  private val ALTER_TO_DIV_EXCEPTIONS = Seq("DIV", "ARTICLE", "SECTION", "P")

  private val IDS_TO_PRESERVE = Seq("readability-content", "readability-page-1")

  private val CLASSES_TO_PRESERVE = Seq("readability-styled", "page")

  private val FLAG_STRIP_UNLIKELYS: Int = 0x1
  private val FLAG_WEIGHT_CLASSES: Int = 0x2
  private val FLAG_CLEAN_CONDITIONALLY: Int = 0x4

  private val dataTableAttribute = "_readabilityDataTable"
  private val contentScoreAttribute = "_readabilityContentScore"

  case class Article(uri: String,
                     title: String,
                     byline: String,
                     content: String,
                     textContent: String,
                     length: Int,
                     excerpt: String,
                     faviconUrl: String,
                     imageUrl: String)

}