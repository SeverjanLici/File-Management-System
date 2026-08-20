import * as React from "react"

const ScrollArea = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={`overflow-y-auto overflow-x-hidden ${className || ""}`}
    {...props}
  />
))
ScrollArea.displayName = "ScrollArea"

export { ScrollArea }
