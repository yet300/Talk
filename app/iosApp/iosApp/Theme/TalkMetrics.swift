import SwiftUI
import SharedLogic

/// `TalkTokens.Spacing` in points.
enum TalkSpacing {
    private static let t = TalkTokens.Spacing.shared
    static let xs = CGFloat(t.xs)
    static let sm = CGFloat(t.sm)
    static let md = CGFloat(t.md)
    static let lg = CGFloat(t.lg)
    static let xl = CGFloat(t.xl)
    static let xxl = CGFloat(t.xxl)
    static let xxxl = CGFloat(t.xxxl)
    static let xxxxl = CGFloat(t.xxxxl)
}

/// `TalkTokens.Radius` in points.
enum TalkRadius {
    private static let t = TalkTokens.Radius.shared
    static let control = CGFloat(t.control)
    static let menu = CGFloat(t.menu)
    static let card = CGFloat(t.card)
}

/// `TalkTokens.Size` in points.
enum TalkSize {
    private static let t = TalkTokens.Size.shared
    static let avatarMd = CGFloat(t.avatarMd)
    static let avatarLg = CGFloat(t.avatarLg)
    static let avatarProfile = CGFloat(t.avatarProfile)
    static let actionTarget = CGFloat(t.actionTarget)
    static let iconAction = CGFloat(t.iconAction)
    static let iconNav = CGFloat(t.iconNav)
    static let iconEmpty = CGFloat(t.iconEmpty)
    static let logo = CGFloat(t.logo)
    static let buttonHeight = CGFloat(t.buttonHeight)
    static let buttonPostHeight = CGFloat(t.buttonPostHeight)
    static let fab = CGFloat(t.fab)
    static let barHeight = CGFloat(t.barHeight)
    static let touchMin = CGFloat(t.touchMin)
    static let bannerHeight = CGFloat(t.bannerHeight)
    static let mediaMax = CGFloat(t.mediaMax)
    static let capsuleHeight = CGFloat(t.capsuleHeight)
    static let capsuleItemWidth = CGFloat(t.capsuleItemWidth)
    static let capsuleItemHeight = CGFloat(t.capsuleItemHeight)
    static let capsuleOffset = CGFloat(t.capsuleOffset)
    static let hairline = CGFloat(t.hairline)
    static let contentMaxWidth = CGFloat(TalkTokens.Layout.shared.colMain)
    static let disabledOpacity = Double(TalkTokens.shared.disabledAlpha)
}
